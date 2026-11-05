/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

package dev.macula.boot.mapstruct;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


/**
 * 在接口 Date / Instant 时间点与实体 LocalDateTime 之间双向转换。
 * 外部配置优先，未配置时使用创建 Bean 时的 JVM 默认时区；由 Mapper 的 uses 显式引用。
 *
 * @author Rain
 * @since 6.1.0
 */
public class DateTimeConverter {

    private final ZoneId storageZone;

    /**
     * 创建双向共用时区的转换器，未配置或留空时沿用 JVM 默认值。
     *
     * @param storageZone 外部配置的时区 ID
     * @throws java.time.DateTimeException 时区 ID 非法时拒绝初始化
     */
    public DateTimeConverter(String storageZone) {
        // 在创建时固定同一时区，保证双向转换一致；属性绑定由自动配置负责。
        this.storageZone = storageZone == null || storageZone.isBlank()
            ? ZoneId.systemDefault() : ZoneId.of(storageZone.trim());
    }

    /**
     * 将接口时间点转换为数据库约定时区的日期时间。
     *
     * @param value 接口时间点，可为 null
     * @return 不携带时区的实体日期时间，输入为空时返回 null
     */
    public LocalDateTime toLocalDateTime(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), storageZone);
    }

    /**
     * 按同一存储时区将数据库日期时间还原为接口时间点。
     *
     * @param value 实体日期时间，可为 null
     * @return 对应的时间点，输入为空时返回 null
     */
    public Date toDate(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(storageZone).toInstant());
    }
    /**
     * 按存储时区将接口Instant转换为实体日期时间，保留纳秒精度。
     *
     * @param value 接口时间点，可为 null
     * @return 实体日期时间，输入为空时返回 null
     */
    public LocalDateTime instantToLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, storageZone);
    }

    /**
     * 按同一存储时区将实体日期时间还原为Instant，不经过毫秒精度的Date。
     *
     * @param value 实体日期时间，可为 null
     * @return 保留纳秒的时间点，输入为空时返回 null
     */
    public Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(storageZone).toInstant();
    }
}
