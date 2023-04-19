/*
 * Copyright (c) 2023 Macula
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

package dev.macula.boot.starter.oss.utils;

import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zip 操作工具类
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品
 *
 * @author 青苗
 * @since 2021-06-22
 */
public class ZipUtils {
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    public static void zipFile(HttpServletResponse response, List<FileEntry> fes) throws IOException {
        if (!StrUtil.isNotEmpty(response.getHeader(CONTENT_DISPOSITION))) {
            String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHmmss"));
            response.setHeader(CONTENT_DISPOSITION, "attachment;filename=" + filename + ".zip");
        }
        zipFile(response.getOutputStream(), fes);
    }

    public static void zipFile(OutputStream os, List<FileEntry> fileEntries) throws IOException {
        ZipOutputStream zos = null;
        try {
            final Map<String, Integer> nameMap = new HashMap<>();
            zos = new ZipOutputStream(os);
            for (FileEntry fileEntry : fileEntries) {
                ZipEntry zipEntry = fileEntry.getZipEntry();
                Integer index = nameMap.get(zipEntry.getName());
                if (null != index) {
                    // 文件名重复处理
                    ++index;
                } else {
                    index = 0;
                }
                nameMap.put(zipEntry.getName(), index);
                if (index > 0) {
                    // 修改文件名
                    ZipEntry ze = new ZipEntry(index + zipEntry.getName());
                    ze.setComment(zipEntry.getComment());
                    fileEntry.setZipEntry(ze);
                }
                fileStreamZip(zos, fileEntry);
            }
        } finally {
            if (null != zos) {
                zos.close();
            }
        }
    }

    /**
     * 压缩文件流
     *
     * @param zos       ZIP 压缩文件输出流
     * @param fileEntry 压缩文件条目
     * @throws IOException IO错误时抛出
     */
    private static void fileStreamZip(ZipOutputStream zos, FileEntry fileEntry) throws IOException {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            zos.putNextEntry(fileEntry.getZipEntry());
            byte[] buffer = new byte[1024 * 10];
            int read;
            int size = buffer.length;
            is = fileEntry.getInputStream();
            bis = new BufferedInputStream(is, size);
            while ((read = bis.read(buffer, 0, size)) != -1) {
                zos.write(buffer, 0, read);
            }
        } finally {
            zos.closeEntry();
            if (null != is) {
                is.close();
            }
            if (null != bis) {
                bis.close();
            }
        }
    }

    @Getter
    @Setter
    @Builder
    public static class FileEntry {
        private InputStream inputStream;
        private ZipEntry zipEntry;

    }
}
