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

package dev.macula.example.consumer.util;

import lombok.Data;

/**
 * {@code Result} 返回结果固定格式
 *
 * @author rain
 * @since 2024/2/27 18:15
 */
@Data
public class Result {
    private boolean success;
    private String errorCode;
    private String errorMsg;
}
