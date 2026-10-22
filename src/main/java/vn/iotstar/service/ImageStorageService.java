package vn.iotstar.service;

import java.io.IOException;
import java.util.Map;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.exception.BusinessException;

@Service
public class ImageStorageService {
    public record UploadedImage(String secureUrl, String publicId) {
    }

    private final Cloudinary cloudinary;

    public ImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public UploadedImage upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new BusinessException("Ảnh phải có dung lượng 1 byte đến 5 MB");
        // Validate magic bytes, not only client-supplied Content-Type or filename.
        byte[] data = file.getBytes();
        boolean jpeg = data.length > 3 && (data[0] & 255) == 255 && (data[1] & 255) == 216 && (data[2] & 255) == 255;
        boolean png = data.length > 8 && (data[0] & 255) == 137 && (data[1] & 255) == 80 && (data[2] & 255) == 78
                && (data[3] & 255) == 71;
        boolean webp = data.length > 12
                && new String(data, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
        if (!(jpeg || png || webp))
            throw new BusinessException("Chỉ cho phép JPEG, PNG và WebP");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(data,
                ObjectUtils.asMap("folder", "techstore/products", "resource_type", "image"));
        return new UploadedImage(String.valueOf(result.get("secure_url")), String.valueOf(result.get("public_id")));
    }

    public void delete(String publicId) throws IOException {
        if (publicId != null && !publicId.isBlank())
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
