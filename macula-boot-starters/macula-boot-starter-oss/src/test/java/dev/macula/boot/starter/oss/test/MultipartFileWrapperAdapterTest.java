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

import dev.macula.boot.starter.oss.file.MultipartFileWrapper;
import dev.macula.boot.starter.oss.file.MultipartFileWrapperAdapter;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * <b>MultipartFileWrapperAdapterTest</b> MultipartFile 包装适配器测试
 * </p>
 * <p>
 * 测试适配器是否正确支持 MultipartFile 类型并正确包装
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class MultipartFileWrapperAdapterTest {

    private final MultipartFileWrapperAdapter adapter = new MultipartFileWrapperAdapter();

    /**
     * 测试支持的类型
     */
    @Test
    void testIsSupport() {
        // MultipartFile 支持
        MultipartFile mockFile = new MockMultipartFile("test", "test.txt", "text/plain", "hello".getBytes());
        Assertions.assertTrue(adapter.isSupport(mockFile));

        // MultipartFileWrapper 也支持
        MultipartFileWrapper wrapper = new MultipartFileWrapper(mockFile, null, null, null);
        Assertions.assertTrue(adapter.isSupport(wrapper));

        // 不支持其他类型
        Assertions.assertFalse(adapter.isSupport("not-a-file"));
        Assertions.assertFalse(adapter.isSupport(null));
        Assertions.assertFalse(adapter.isSupport(new byte[0]));
    }

    /**
     * 测试获取 FileWrapper，自动从 MultipartFile 获取文件名、contentType、大小
     */
    @Test
    void testGetFileWrapperFromMultipartFile() {
        // given
        String originalFilename = "test-file.txt";
        String contentType = "text/plain";
        byte[] content = "Hello World".getBytes();
        MultipartFile mockFile = new MockMultipartFile("file", originalFilename, contentType, content);

        // when
        FileWrapper wrapper = adapter.getFileWrapper(mockFile, null, null, null);

        // then
        Assertions.assertNotNull(wrapper);
        Assertions.assertEquals(originalFilename, wrapper.getName());
        Assertions.assertEquals(contentType, wrapper.getContentType());
        Assertions.assertEquals(content.length, wrapper.getSize());
    }

    /**
     * 测试显式指定名称、contentType、size覆盖MultipartFile中的值
     */
    @Test
    void testGetFileWrapperWithExplicitValues() {
        // given
        MultipartFile mockFile = new MockMultipartFile("file", "original.txt", "text/plain", "Hello World".getBytes());
        String customName = "custom-name.txt";
        String customContentType = "application/json";
        Long customSize = 100L;

        // when
        FileWrapper wrapper = adapter.getFileWrapper(mockFile, customName, customContentType, customSize);

        // then
        Assertions.assertEquals(customName, wrapper.getName());
        Assertions.assertEquals(customContentType, wrapper.getContentType());
        Assertions.assertEquals(customSize, wrapper.getSize());
    }

    /**
     * 测试从已有的 MultipartFileWrapper 更新
     */
    @Test
    void testGetFileWrapperFromExistingWrapper() {
        // given
        MultipartFile mockFile = new MockMultipartFile("file", "original.txt", "text/plain", "Hello World".getBytes());
        MultipartFileWrapper existingWrapper = new MultipartFileWrapper(mockFile, "old-name.txt", "text/plain", 11L);
        String newName = "new-name.txt";

        // when
        FileWrapper updatedWrapper = adapter.getFileWrapper(existingWrapper, newName, null, null);

        // then
        Assertions.assertEquals(newName, updatedWrapper.getName());
    }

    /**
     * 测试null名称使用original filename
     */
    @Test
    void testNullNameUsesOriginalFilename() {
        // given
        MultipartFile mockFile = new MockMultipartFile("file", "original-name.jpg", "image/jpeg", new byte[100]);

        // when
        FileWrapper wrapper = adapter.getFileWrapper(mockFile, null, "image/png", 200L);

        // then
        Assertions.assertEquals("original-name.jpg", wrapper.getName());
        Assertions.assertEquals("image/png", wrapper.getContentType());
        Assertions.assertEquals(200L, wrapper.getSize());
    }
}
