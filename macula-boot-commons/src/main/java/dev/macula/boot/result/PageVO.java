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

package dev.macula.boot.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * {@code PageVo} 分页返回VO
 *
 * @author rain
 * @since 2023/6/21 15:45
 */
@Data
public class PageVO<T> implements Serializable {
    /**
     * 当前页的数据
     */
    private List<T> records;

    /**
     * 总数
     */
    private long total;

    /**
     * 每页记录数
     */
    private long size;

    /**
     * 当前页码，建议从1开始
     */
    private long current;
}
