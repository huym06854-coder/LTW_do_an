package vn.iotstar.service;

import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.domain.entity.ProductImage;
import vn.iotstar.exception.ResourceNotFoundException;
import vn.iotstar.repository.ProductImageRepository;
import vn.iotstar.repository.ProductRepository;

@Service
public class ProductImageService {
    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ImageStorageService storage;

    public ProductImageService(ProductRepository products, ProductImageRepository images, ImageStorageService storage) {
        this.products = products;
        this.images = images;
        this.storage = storage;
    }

    @Transactional
    public void replacePrimary(Long productId, MultipartFile file) throws IOException {
        var product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        var old = images.findByProductIdOrderBySortOrderAsc(productId).stream().filter(ProductImage::isPrimary)
                .findFirst().orElse(null);
        var uploaded = storage.upload(file); // Outside DB: compensate on rollback
        String oldPublicId = old == null ? null : old.getCloudinaryPublicId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                try {
                    if (status != STATUS_COMMITTED)
                        storage.delete(uploaded.publicId());
                    else if (oldPublicId != null)
                        storage.delete(oldPublicId);
                } catch (IOException error) {
                    // Cloud cleanup failures need operator reconciliation (do not roll back a
                    // committed DB transaction).
                    System.err.println("Cloudinary cleanup failed for product " + productId);
                }
            }
        });
        if (old != null) {
            // Remove and flush the old primary first to satisfy the filtered unique index.
            // The old Cloudinary object is retained until the database transaction commits.
            images.delete(old);
            images.flush();
        }
        ProductImage created = new ProductImage();
        created.setProduct(product);
        created.setPrimary(true);
        created.setSortOrder(0);
        created.setImageUrl(uploaded.secureUrl());
        created.setCloudinaryPublicId(uploaded.publicId());
        images.saveAndFlush(created);
    }
}
