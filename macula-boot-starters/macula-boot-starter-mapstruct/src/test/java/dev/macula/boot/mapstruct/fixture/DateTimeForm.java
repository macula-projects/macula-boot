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

package dev.macula.boot.mapstruct.fixture;

import io.github.linpeilie.annotations.AutoMapper;
import java.util.Date;
import java.time.Instant;
import lombok.Data;

/**
 * 接口时间点测试模型；依赖测试模块的 Plus 全局配置。
 * @author Rain
 * @since 6.1.0
 */
@Data
@AutoMapper(target = DateTimeEntity.class)
public class DateTimeForm {
    private Date createTime;
    private Instant occurredAt;
}
