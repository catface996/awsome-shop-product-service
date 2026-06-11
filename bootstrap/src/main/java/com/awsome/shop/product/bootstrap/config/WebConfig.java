package com.awsome.shop.product.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：将上传的商品图片静态目录映射为可访问的 URL。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${awsomeshop.image.storage-path:./data/product-images}")
    private String storagePath;

    @Value("${awsomeshop.image.url-prefix:/images}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        String location = "file:" + (storagePath.endsWith("/") ? storagePath : storagePath + "/");
        registry.addResourceHandler(prefix + "/**").addResourceLocations(location);
    }
}
