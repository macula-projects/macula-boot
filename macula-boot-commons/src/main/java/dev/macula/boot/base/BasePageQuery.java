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

package dev.macula.boot.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * {@code BasePageQuery} 带分页查询的基类
 *
 * @author rain
 * @since 2022/12/21 13:47
 */
@Data
@Schema
public class BasePageQuery {

    /**
     * 默认构造函数
     */
    public BasePageQuery() {
    }

    @Schema(description = "页码", example = "1")
    private int pageNum = 1;

    @Schema(description = "每页记录数", example = "10")
    private int pageSize = 10;
}
