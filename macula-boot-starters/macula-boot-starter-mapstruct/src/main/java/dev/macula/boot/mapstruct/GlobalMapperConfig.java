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

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;

/**
 * 普通 MapStruct 与 MapStruct Plus 的共享编译配置。
 * Parent 默认编译参数让 Plus 引用此配置，运行时转换器由自动配置注册。
 *
 * @author Rain
 * @since 6.1.0
 */
@io.github.linpeilie.annotations.MapperConfig(uses = DateTimeConverter.class)
@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING, uses = DateTimeConverter.class)
public interface GlobalMapperConfig {
}
