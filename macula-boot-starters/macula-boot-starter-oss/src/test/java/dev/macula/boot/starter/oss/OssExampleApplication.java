/*
 * Copyright (c) 2022 Macula
 *   macula.dev, China
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

package dev.macula.boot.starter.oss;

import dev.macula.boot.starter.oss.config.OssProperty;
import dev.macula.boot.starter.oss.model.StoragePlatform;
import dev.macula.boot.starter.oss.platform.Minio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OssExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OssExampleApplication.class, args);
    }

    @Bean
    public IFileStorage minio3() {
        // 注入一个自定义存储平台
        OssProperty ossProperty = new OssProperty();
        ossProperty.setPlatform(StoragePlatform.minio);
        ossProperty.setBucketName("test3");
        ossProperty.setEndpoint("http://127.0.0.1:9019");
        ossProperty.setAccessKey("q7RNi6elbvQ0j1ry");
        ossProperty.setSecretKey("HMoKkeu0zGSvSdDGWlMDuytaRON12St9");
        return new Minio(ossProperty);
    }
}
