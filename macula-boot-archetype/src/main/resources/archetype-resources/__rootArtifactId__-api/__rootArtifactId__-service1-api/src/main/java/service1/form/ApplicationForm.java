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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service1.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "应用表单对象")
@Data
public class ApplicationForm {

    @Schema(description = "应用名称")
    @NotBlank(message = "应用名称不能为空")
    private String applicationName;

    @Schema(description = "ak")
    private String ak;

    @Schema(description = "sk")
    private String sk;

    @Schema(description = "主页")
    private String homepage;

    @Schema(description = "应用编码")
    @NotBlank(message = "应用编码不能为空")
    private String code;

    @Schema(description = "可访问路径")
    private String accessPath;

    @Schema(description = "负责人")
    @NotBlank(message = "负责人不能为空")
    private String manager;

    @Schema(description = "维护人")
    private String maintainer;

    @Pattern(regexp = "^1(3\\d|4[5-9]|5[0-35-9]|6[2567]|7[0-8]|8\\d|9[0-35-9])\\d{8}$", message = "{phone.valid}")
    private String mobile;

    @Schema(description = "是否回传属性， true：回传否则不回传")
    private boolean useAttrs;

    @Schema(description = "回传属性列表")
    private String allowedAttrs;
}
