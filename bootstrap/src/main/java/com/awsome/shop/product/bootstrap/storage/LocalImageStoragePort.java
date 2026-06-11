package com.awsome.shop.product.bootstrap.storage;

import com.awsome.shop.product.common.enums.ProductErrorCode;
import com.awsome.shop.product.common.exception.SystemException;
import com.awsome.shop.product.storage.ImageStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件卷图片存储适配器（实现 domain 的 {@link ImageStoragePort}）。
 *
 * <p>将图片写入本地卷目录（{@code awsomeshop.image.storage-path}），并返回由
 * {@code awsomeshop.image.url-prefix} 前缀拼接的可访问 URL；静态资源映射见 WebConfig。</p>
 */
@Component
public class LocalImageStoragePort implements ImageStoragePort {

    @Value("${awsomeshop.image.storage-path:./data/product-images}")
    private String storagePath;

    @Value("${awsomeshop.image.url-prefix:/images}")
    private String urlPrefix;

    @Override
    public String store(byte[] content, String originalFilename) {
        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);

            String filename = UUID.randomUUID().toString().replace("-", "") + extractExtension(originalFilename);
            Path target = dir.resolve(filename);
            Files.write(target, content);

            String prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
            return prefix + "/" + filename;
        } catch (IOException e) {
            throw new SystemException(ProductErrorCode.IMAGE_UPLOAD_FAILED, e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }
}
