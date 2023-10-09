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
package ${package}.service1.pojo.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 应用业务对象
 */
@Data
public class ApplicationBO {

    private Long id;

    /**
     * 应用名
     */
    private String applicationName;

    private String ak;

    private String sk;

    /**
     * 主页
     */
    private String homepage;

    /**
     * 负责人
     */
    private String manager;

    /**
     * 维护人
     */
    private String maintainer;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 应用编码
     */
    private String code;

    /**
     * 可访问url
     */
    private String accessPath;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createTime;

    /**
     * 是否回传属性
     */
    private boolean useAttrs;

    /**
     * 回传属性列表
     */
    private String allowedAttrs;
}
