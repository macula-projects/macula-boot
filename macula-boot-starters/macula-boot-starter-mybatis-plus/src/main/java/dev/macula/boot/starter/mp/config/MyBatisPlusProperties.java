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

package dev.macula.boot.starter.mp.config;

import dev.macula.boot.constants.GlobalConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * <b>MyBatisPlusProperties</b> MyBatis Plus模块的可配置属性
 * </p>
 *
 * @author Rain
 * @since 2022-01-22
 */
@ConfigurationProperties(prefix = "macula.mybatis-plus")
@Getter
@Setter
public class MyBatisPlusProperties {

    private Audit audit = new Audit();

    private long tenantId = GlobalConstants.DEFAULT_TENANT_ID;

    private String[] tenantSuffixes = new String[] {"tenant", "TENANT"};

    private boolean tenantEnable = true;

    /**
     * 分页时允许的最大记录数
     */
    private long maxLimit = 1000L;


    @Getter
    @Setter
    public static class Audit {

        private String createTimeName = "createTime";
        private String createByName = "createBy";
        private String lastUpdateTimeName = "lastUpdateTime";
        private String lastUpdateByName = "lastUpdateBy";
    }
}
