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

package dev.macula.boot.starter.tinyid.base.entity;

/**
 * @author du_imba
 */
public class ResultCode {

    /**
     * 正常可用
     */
    public static final int NORMAL = 1;
    /**
     * 需要去加载nextId
     */
    public static final int LOADING = 2;
    /**
     * 超过maxId 不可用
     */
    public static final int OVER = 3;

    private ResultCode() {

    }
}
