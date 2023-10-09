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
package ${package}.service1.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import dev.macula.boot.starter.mp.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("application")
public class Application extends BaseEntity {

    /**
     * 应用名称
     */
    private String applicationName;

    /**
     * 应用主页URL
     */
    private String homepage;

    /**
     * 应用AppKey
     */
    private String ak;

    /**
     * 应用SecretKey
     */
    private String sk;

    /**
     * 负责人
     */
    private String manager;

    /**
     * 维护人
     */
    private String maintainer;

    /**
     * 移动电话
     */
    private String mobile;

    /**
     * 应用编码
     */
    private String code;

    /**
     * 可访问路径正则
     */
    private String accessPath;

    /**
     * 是否允许回传属性
     */
    private boolean useAttrs;

    /**
     * 允许回传的属性
     */
    private String allowedAttrs;
}
