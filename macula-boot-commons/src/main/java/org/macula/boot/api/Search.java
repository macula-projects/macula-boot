/*
 * Copyright (c) 2022 Macula
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

package org.macula.boot.api;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 搜索封装类
 *
 * @author pangu
 */
@Getter
@Setter
public class Search implements Serializable {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 排序属性
     */
    private String prop;

    /**
     * 排序方式：asc,desc
     */
    private String order;

    /**
     * 当前页
     */
    private Integer current = 1;

    /**
     * 每页的数量
     */
    private Integer size = 10;
}
