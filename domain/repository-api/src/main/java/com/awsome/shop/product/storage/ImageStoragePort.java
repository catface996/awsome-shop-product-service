package com.awsome.shop.product.storage;

/**
 * 图片存储端口（Domain → Infrastructure）
 *
 * <p>保持框架无关：以字节数组 + 原始文件名为入参，由基础设施适配器决定具体存储位置
 * （本地卷 / 对象存储等），返回可访问的 URL。</p>
 */
public interface ImageStoragePort {

    /**
     * 存储图片并返回访问 URL。
     *
     * @param content          图片二进制内容
     * @param originalFilename 原始文件名（用于提取扩展名）
     * @return 可访问的图片 URL
     */
    String store(byte[] content, String originalFilename);
}
