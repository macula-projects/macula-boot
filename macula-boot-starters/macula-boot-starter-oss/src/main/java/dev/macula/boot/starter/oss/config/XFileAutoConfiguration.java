/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.starter.oss.config;

import dev.macula.boot.starter.oss.file.MultipartFileWrapperAdapter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.aspect.FileStorageAspect;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorageClientFactory;
import org.dromara.x.file.storage.core.recorder.DefaultFileRecorder;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.tika.DefaultTikaFactory;
import org.dromara.x.file.storage.core.tika.TikaContentTypeDetect;
import org.dromara.x.file.storage.core.tika.TikaFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

import static org.dromara.x.file.storage.core.FileStorageServiceBuilder.doesNotExistClass;

/**
 * {@code XFileAutoConfiguration} 自动化配置
 *
 * @author rain
 * @since 2024/1/31 11:03
 */
@Slf4j
@AutoConfiguration
@ConditionalOnMissingBean(FileStorageService.class)
@EnableConfigurationProperties(XFileProperties.class)
public class XFileAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
    public Object fileStorageWebMvcConfigurer(XFileProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                for (XFileProperties.SpringLocalConfig local : properties.getLocal()) {
                    if (local.getEnableStorage() && local.getEnableAccess()) {
                        registry.addResourceHandler(local.getPathPatterns())
                            .addResourceLocations("file:" + local.getBasePath());
                    }
                }
                for (XFileProperties.SpringLocalPlusConfig local : properties.getLocalPlus()) {
                    if (local.getEnableStorage() && local.getEnableAccess()) {
                        registry.addResourceHandler(local.getPathPatterns())
                            .addResourceLocations("file:" + local.getStoragePath());
                    }
                }
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(FileRecorder.class)
    public FileRecorder fileRecorder() {
        log.warn("没有找到 FileRecorder 的实现类，文件上传之外的部分功能无法正常使用，必须实现该接口才能使用完整功能！");
        return new DefaultFileRecorder();
    }

    @Bean
    @ConditionalOnMissingBean(TikaFactory.class)
    public TikaFactory tikaFactory() {
        return new DefaultTikaFactory();
    }

    @Bean
    @ConditionalOnMissingBean(ContentTypeDetect.class)
    public ContentTypeDetect contentTypeDetect(TikaFactory tikaFactory) {
        return new TikaContentTypeDetect(tikaFactory);
    }

    @Bean(destroyMethod = "destroy")
    public FileStorageService fileStorageService(FileRecorder fileRecorder,
        @Autowired(required = false) List<List<? extends FileStorage>> fileStorageLists,
        @Autowired(required = false) List<FileStorageAspect> aspectList,
        @Autowired(required = false) List<FileWrapperAdapter> fileWrapperAdapterList,
        ContentTypeDetect contentTypeDetect,
        @Autowired(required = false) List<List<FileStorageClientFactory<?>>> clientFactoryList,
        XFileProperties properties) {

        if (fileStorageLists == null)
            fileStorageLists = new ArrayList<>();
        if (aspectList == null)
            aspectList = new ArrayList<>();
        if (fileWrapperAdapterList == null)
            fileWrapperAdapterList = new ArrayList<>();
        if (clientFactoryList == null)
            clientFactoryList = new ArrayList<>();

        FileStorageServiceBuilder builder =
            FileStorageServiceBuilder.create(properties.toFileStorageProperties()).setFileRecorder(fileRecorder)
                .setAspectList(aspectList).setContentTypeDetect(contentTypeDetect)
                .setFileWrapperAdapterList(fileWrapperAdapterList).setClientFactoryList(clientFactoryList);

        fileStorageLists.forEach(builder::addFileStorage);

        if (properties.getEnableByteFileWrapper()) {
            builder.addByteFileWrapperAdapter();
        }
        if (properties.getEnableUriFileWrapper()) {
            builder.addUriFileWrapperAdapter();
        }
        if (properties.getEnableInputStreamFileWrapper()) {
            builder.addInputStreamFileWrapperAdapter();
        }
        if (properties.getEnableLocalFileWrapper()) {
            builder.addLocalFileWrapperAdapter();
        }
        if (properties.getEnableHttpServletRequestFileWrapper()) {
            if (doesNotExistClass("javax.servlet.http.HttpServletRequest") && doesNotExistClass(
                "jakarta.servlet.http.HttpServletRequest")) {
                log.warn(
                    "当前未检测到 Servlet 环境，无法加载 HttpServletRequest 的文件包装适配器，请将参数【dromara.x-file-storage.enable-http-servlet-request-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addHttpServletRequestFileWrapperAdapter();
            }
        }
        if (properties.getEnableMultipartFileWrapper()) {
            if (doesNotExistClass("org.springframework.web.multipart.MultipartFile")) {
                log.warn(
                    "当前未检测到 SpringWeb 环境，无法加载 MultipartFile 的文件包装适配器，请将参数【dromara.x-file-storage.enable-multipart-file-wrapper】设置为 【false】来消除此警告");
            } else {
                builder.addFileWrapperAdapter(new MultipartFileWrapperAdapter());
            }
        }

        if (doesNotExistClass("org.springframework.web.servlet.config.annotation.WebMvcConfigurer")) {
            long localAccessNum =
                properties.getLocal().stream().filter(XFileProperties.SpringLocalConfig::getEnableStorage)
                    .filter(XFileProperties.SpringLocalConfig::getEnableAccess).count();
            long localPlusAccessNum =
                properties.getLocalPlus().stream().filter(XFileProperties.SpringLocalPlusConfig::getEnableStorage)
                    .filter(XFileProperties.SpringLocalPlusConfig::getEnableAccess).count();

            if (localAccessNum + localPlusAccessNum > 0) {
                log.warn("当前未检测到 SpringWeb 环境，无法开启本地存储平台的本地访问功能，请将关闭本地访问来消除此警告");
            }
        }

        return builder.build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent() {
        FileStorageService service = applicationContext.getBean(FileStorageService.class);
        service.setSelf(service);
    }
}
