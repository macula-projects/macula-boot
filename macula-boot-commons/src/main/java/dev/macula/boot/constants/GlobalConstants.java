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

package dev.macula.boot.constants;

/**
 * {@code GlobalConstants} 全局常量
 *
 * @author rain
 * @since 2023/3/1 19:18
 */
public interface GlobalConstants {
    /** 请求ID */
    String FEIGN_REQ_ID = "FEIGN_REQ_ID";

    /** 租户ID */
    String TENANT_ID_NAME = "tenantId";

    /** 令牌ID */
    String TOKEN_ID_NAME = "tokenId";

    /** 默认租户ID */
    Long DEFAULT_TENANT_ID = 1L;

    /** 灰度版本标签 */
    String GRAY_VERSION_TAG = "grayversion";
}
