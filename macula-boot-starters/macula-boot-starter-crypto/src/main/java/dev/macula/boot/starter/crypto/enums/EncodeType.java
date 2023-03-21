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

package dev.macula.boot.starter.crypto.enums;

/**
 * 编码类型
 *
 * @author 老马
 * @version 4.6.0
 */
public enum EncodeType {

    /**
     * 默认使用yml配置
     */
    DEFAULT,

    /**
     * base64编码
     */
    BASE64,

    /**
     * 16进制编码
     */
    HEX;

}
