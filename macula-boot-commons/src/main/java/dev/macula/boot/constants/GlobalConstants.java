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

package dev.macula.boot.constants;

/**
 * <p>
 * <b>GlobalConstants</b> 全局常量
 * </p>
 *
 * @author Rain
 * @since 2022-02-20
 */
public interface GlobalConstants {

    /**
     * 根部门ID
     */
    Long ROOT_NODE_ID = 0l;


    /**
     * 系统默认密码
     */
    String DEFAULT_USER_PASSWORD = "123456";

    /**
     * 超级管理员角色编码
     */
    String ROOT_ROLE_CODE = "ROOT";

    /**
     * [ {接口路径:[角色编码]},...]
     */
    String URL_PERM_ROLES_KEY = "system:perm_roles_rule:url";

    String AUTH_USER_PERMS_KEY = "system:auth:user_perms:";

    String FEIGN_REQ_ID = "FEIGN_REQ_ID";
}
