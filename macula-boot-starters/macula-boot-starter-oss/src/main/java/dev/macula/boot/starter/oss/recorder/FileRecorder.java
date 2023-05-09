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

package dev.macula.boot.starter.oss.recorder;

import dev.macula.boot.starter.oss.support.FileInfo;

/**
 * 文件记录记录者接口
 */
public interface FileRecorder {

    /**
     * 保存文件记录
     */
    boolean record(FileInfo fileInfo);

    /**
     * 根据 url 获取文件记录
     */
    FileInfo getByUrl(String url);

    /**
     * 根据 url 删除文件记录
     */
    boolean delete(String url);
}
