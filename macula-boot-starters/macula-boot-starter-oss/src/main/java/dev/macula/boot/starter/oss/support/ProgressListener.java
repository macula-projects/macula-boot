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

package dev.macula.boot.starter.oss.support;

/**
 * 进度监听器
 */
public interface ProgressListener {

    /**
     * 开始
     */
    void start();

    /**
     * 进行中
     *
     * @param progressSize 已经进行的大小
     * @param allSize      总大小，来自 fileInfo.getSize()
     */
    void progress(long progressSize, long allSize);

    /**
     * 结束
     */
    void finish();
}
