package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.domain.entity.Product;
import vn.iotstar.domain.entity.ProductImage;
import vn.iotstar.repository.ProductImageRepository;
import vn.iotstar.repository.ProductRepository;

class ProductImageServiceTest {

    @Test
    void replacementFlushesRemovalBeforeInsertAndDeletesOldCloudAssetAfterCommit() throws Exception {
        ProductRepository products = mock(ProductRepository.class);
        ProductImageRepository images = mock(ProductImageRepository.class);
        ImageStorageService storage = mock(ImageStorageService.class);
        Product product = new Product();
        ProductImage oldImage = new ProductImage();
        oldImage.setPrimary(true);
        oldImage.setCloudinaryPublicId("old-public-id");

        when(products.findById(7L)).thenReturn(Optional.of(product));
        when(images.findByProductIdOrderBySortOrderAsc(7L)).thenReturn(List.of(oldImage));
        when(storage.upload(any(MultipartFile.class)))
                .thenReturn(new ImageStorageService.UploadedImage("https://example.test/new.jpg", "new-public-id"));

        ProductImageService service = new ProductImageService(products, images, storage);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.replacePrimary(7L, mock(MultipartFile.class));

            InOrder order = inOrder(images);
            order.verify(images).delete(oldImage);
            order.verify(images).flush();
            ArgumentCaptor<ProductImage> created = ArgumentCaptor.forClass(ProductImage.class);
            order.verify(images).saveAndFlush(created.capture());
            assertTrue(created.getValue().isPrimary());
            assertEquals("new-public-id", created.getValue().getCloudinaryPublicId());

            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
            verify(storage).delete("old-public-id");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
