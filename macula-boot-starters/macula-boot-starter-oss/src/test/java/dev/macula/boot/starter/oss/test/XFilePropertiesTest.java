/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.oss.test;

import dev.macula.boot.starter.oss.config.XFileProperties;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * <p>
 * <b>XFilePropertiesTest</b> X-File-Storage 属性配置测试
 * </p>
 * <p>
 * 测试配置属性绑定正确，toFileStorageProperties 转换方法正确工作
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@SpringBootTest(properties = {
        "macula.oss.default-platform=minio",
        "macula.oss.thumbnailSuffix=.thumb.jpg",
        "macula.oss.upload-not-support-metadata-throw-exception=false",
        "macula.oss.minio[0].enable-storage=true",
        "macula.oss.minio[0].access-key=minioadmin",
        "macula.oss.minio[0].secret-key=minioadmin",
        "macula.oss.minio[0].endpoint=http://127.0.0.1:9000",
        "macula.oss.minio[0].bucket-name=test-bucket",
        "macula.oss.aliyun-oss[0].enable-storage=false",
        "macula.oss.aliyun-oss[0].bucket-name=disabled-bucket"
})
public class XFilePropertiesTest {

    @Configuration
    @EnableConfigurationProperties(XFileProperties.class)
    static class TestConfig {
    }

    @org.springframework.beans.factory.annotation.Autowired
    private XFileProperties properties;

    /**
     * 测试默认值
     */
    @Test
    void testDefaultValues() {
        XFileProperties defaultProperties = new XFileProperties();
        Assertions.assertEquals("local", defaultProperties.getDefaultPlatform());
        Assertions.assertEquals(".min.jpg", defaultProperties.getThumbnailSuffix());
        Assertions.assertTrue(defaultProperties.getUploadNotSupportMetadataThrowException());
        Assertions.assertTrue(defaultProperties.getEnableByteFileWrapper());
        Assertions.assertTrue(defaultProperties.getEnableMultipartFileWrapper());
        Assertions.assertTrue(defaultProperties.getLocal().isEmpty());
        Assertions.assertTrue(defaultProperties.getLocalPlus().isEmpty());
    }

    /**
     * 测试配置绑定
     */
    @Test
    void testConfigurationBinding() {
        // 验证基本属性
        Assertions.assertEquals("minio", properties.getDefaultPlatform());
        Assertions.assertEquals(".thumb.jpg", properties.getThumbnailSuffix());
        Assertions.assertEquals(false, properties.getUploadNotSupportMetadataThrowException());

        // 验证MinIO配置
        var minioConfigs = properties.getMinio();
        Assertions.assertEquals(1, minioConfigs.size());
        XFileProperties.SpringMinioConfig minioConfig = minioConfigs.get(0);
        Assertions.assertTrue(minioConfig.getEnableStorage());
        Assertions.assertEquals("minioadmin", minioConfig.getAccessKey());
        Assertions.assertEquals("minioadmin", minioConfig.getSecretKey());
        Assertions.assertEquals("http://127.0.0.1:9000", minioConfig.getEndPoint());
        Assertions.assertEquals("test-bucket", minioConfig.getBucketName());

        // 验证阿里云OSS配置（已禁用）
        var aliyunConfigs = properties.getAliyunOss();
        Assertions.assertEquals(1, aliyunConfigs.size());
        Assertions.assertFalse(aliyunConfigs.get(0).getEnableStorage());
    }

    /**
     * 测试转换为FileStorageProperties，只转换启用的存储配置
     */
    @Test
    void testToFileStoragePropertiesConversion() {
        FileStorageProperties fileStorageProperties = properties.toFileStorageProperties();

        // 验证基本属性转换
        Assertions.assertEquals(properties.getDefaultPlatform(), fileStorageProperties.getDefaultPlatform());
        Assertions.assertEquals(properties.getThumbnailSuffix(), fileStorageProperties.getThumbnailSuffix());
        Assertions.assertEquals(properties.getUploadNotSupportMetadataThrowException(),
                fileStorageProperties.getUploadNotSupportMetadataThrowException());

        // 验证只包含启用的存储配置（MinIO是启用，阿里云是禁用）
        var convertedMinioConfigs = fileStorageProperties.getMinio();
        Assertions.assertEquals(1, convertedMinioConfigs.size());
        Assertions.assertEquals("minioadmin", convertedMinioConfigs.get(0).getAccessKey());

        var convertedAliyunConfigs = fileStorageProperties.getAliyunOss();
        // 禁用的配置不应该被转换进去
        Assertions.assertTrue(convertedAliyunConfigs.isEmpty());
    }

    /**
     * 测试所有存储配置类型都能正确转换
     */
    @Test
    void testAllStorageConfigsConversion() {
        // given: 添加一个启用的本地存储
        XFileProperties properties = new XFileProperties();
        XFileProperties.SpringLocalPlusConfig localConfig = new XFileProperties.SpringLocalPlusConfig();
        localConfig.setEnableStorage(true);
        localConfig.setBasePath("/tmp/files");
        properties.getLocalPlus().add(localConfig);

        // when: 转换
        FileStorageProperties converted = properties.toFileStorageProperties();

        // then: 本地配置转换正确
        Assertions.assertEquals(1, converted.getLocalPlus().size());
        Assertions.assertEquals("/tmp/files", converted.getLocalPlus().get(0).getBasePath());
    }
}
