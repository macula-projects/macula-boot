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

package dev.macula.boot.starter.oss.file;

import lombok.Getter;
import lombok.Setter;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code MultipartFileWrapperAdapter} 文件包装适配器
 *
 * @author rain
 * @since 2024/1/31 11:01
 */
@Getter
@Setter
public class MultipartFileWrapperAdapter implements FileWrapperAdapter {

    @Override
    public boolean isSupport(Object source) {
        return source instanceof MultipartFile || source instanceof MultipartFileWrapper;
    }

    @Override
    public FileWrapper getFileWrapper(Object source, String name, String contentType, Long size) {
        if (source instanceof MultipartFileWrapper) {
            return updateFileWrapper((MultipartFileWrapper)source, name, contentType, size);
        } else {
            MultipartFile file = (MultipartFile)source;
            if (name == null)
                name = file.getOriginalFilename();
            if (contentType == null)
                contentType = file.getContentType();
            if (size == null)
                size = file.getSize();
            return new MultipartFileWrapper(file, name, contentType, size);
        }
    }
}
