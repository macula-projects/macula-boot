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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service1.vo.app;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用分页视图对象
 */
@Schema(description = "应用分页视图对象")
@Data
public class ApplicationVO {

    @Schema(description = "应用ID")
    private Long id;

    @Schema(description = "应用名")
    private String applicationName;

    private String ak;

    private String sk;

    @Schema(description = "主页")
    private String homepage;

    @Schema(description = "负责人")
    private String manager;

    @Schema(description = "维护人")
    private String maintainer;

    @Schema(description = "联系方式")
    private String mobile;

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "可访问url")
    private String accessPath;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH24:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "是否回传属性")
    private boolean useAttrs;

    @Schema(description = "回传属性列表")
    private String allowedAttrs;

}
