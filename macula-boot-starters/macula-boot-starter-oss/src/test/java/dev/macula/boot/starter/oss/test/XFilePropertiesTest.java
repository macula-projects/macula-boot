/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.starter.oss.config.XFileProperties;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * {@code XFilePropertiesTest} 文件存储配置属性单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class XFilePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues("macula.oss.default-platform=minio", "macula.oss.thumbnail-suffix=.thumb.jpg", "macula.oss.upload-not-support-metadata-throw-exception=false", "macula.oss.minio[0].enable-storage=true", "macula.oss.minio[0].access-key=minioadmin", "macula.oss.minio[0].secret-key=minioadmin", "macula.oss.minio[0].endpoint=http://127.0.0.1:9000", "macula.oss.minio[0].bucket-name=test-bucket", "macula.oss.aliyun-oss[0].enable-storage=false", "macula.oss.aliyun-oss[0].bucket-name=disabled-bucket");

    @Test
    void hasDocumentedDefaults() {
        XFileProperties properties = new XFileProperties();

        assertThat(properties.getDefaultPlatform()).isEqualTo("local");
        assertThat(properties.getThumbnailSuffix()).isEqualTo(".min.jpg");
        assertThat(properties.getUploadNotSupportMetadataThrowException()).isTrue();
        assertThat(properties.getEnableByteFileWrapper()).isTrue();
        assertThat(properties.getEnableMultipartFileWrapper()).isTrue();
        assertThat(properties.getLocal()).isEmpty();
        assertThat(properties.getLocalPlus()).isEmpty();
    }

    @Test
    void bindsStorageProperties() {
        contextRunner.run(context -> {
            XFileProperties properties = context.getBean(XFileProperties.class);

            assertThat(properties.getDefaultPlatform()).isEqualTo("minio");
            assertThat(properties.getThumbnailSuffix()).isEqualTo(".thumb.jpg");
            assertThat(properties.getUploadNotSupportMetadataThrowException()).isFalse();
            assertThat(properties.getMinio()).singleElement().satisfies(minio -> {
                assertThat(minio.getEnableStorage()).isTrue();
                assertThat(minio.getAccessKey()).isEqualTo("minioadmin");
                assertThat(minio.getSecretKey()).isEqualTo("minioadmin");
                assertThat(minio.getEndPoint()).isEqualTo("http://127.0.0.1:9000");
                assertThat(minio.getBucketName()).isEqualTo("test-bucket");
            });
            assertThat(properties.getAliyunOss()).singleElement()
                .satisfies(aliyun -> assertThat(aliyun.getEnableStorage()).isFalse());
        });
    }

    @Test
    void convertsOnlyEnabledStorageConfigurations() {
        contextRunner.run(context -> {
            FileStorageProperties converted = context.getBean(XFileProperties.class).toFileStorageProperties();

            assertThat(converted.getDefaultPlatform()).isEqualTo("minio");
            assertThat(converted.getMinio()).singleElement()
                .satisfies(minio -> assertThat(minio.getAccessKey()).isEqualTo("minioadmin"));
            assertThat(converted.getAliyunOss()).isEmpty();
        });
    }

    @Test
    void convertsEnabledLocalPlusConfiguration() {
        XFileProperties properties = new XFileProperties();
        XFileProperties.SpringLocalPlusConfig local = new XFileProperties.SpringLocalPlusConfig();
        local.setEnableStorage(true);
        local.setBasePath("/tmp/files");
        properties.getLocalPlus().add(local);

        assertThat(properties.toFileStorageProperties().getLocalPlus()).singleElement()
            .satisfies(converted -> assertThat(converted.getBasePath()).isEqualTo("/tmp/files"));
    }

    /**
     * 测试配置，用于注册文件存储配置属性。
     *
     * @since 2026/8/12
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(XFileProperties.class)
    static class PropertiesConfiguration {
    }
}
