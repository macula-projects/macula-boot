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

package dev.macula.boot.starter.cloud.gateway.constants;

/**
 * {@code GatewayConstant} 网关常量
 *
 * @author rain
 * @since 2023/2/20 11:52
 */
public interface GatewayConstants {

    String CACHED_REQUEST_BODY_OBJECT_KEY = "CACHED_REQUEST_BODY_OBJECT_KEY";

    String HMAC_AUTH_PREFIX = "hmac username";

    String PROTECT_URLS_ENDPOINT = "/gateway/protect/urls";

    String PROTECT_KEY_ENDPOINT = "/gateway/protect/key";

    String SM4_KEY = "sm4-key";

    String SYM_ALG = "sym-alg";

    String CRYPTO_SWITCH = "crypto-switch";

    String SIGN_SWITCH = "sign-switch";

    String CRYPTO_DATA_KEY = "data";

    String REMOTE_INVOKE_NAME = "invoker";

    String SIGNATURE_NAME = "signature";
    String TIMESTAMP_NAME = "timestamp";
    String NONCE_NAME = "nonce";
    String ALGORITHM_NAME = "algorithm";
    String DEFAULT_ALGORITHM = "SHA-256";
    long DEFAULT_TIMESTAMP_BTW = 300;
    String DEFAULT_RM_OPAQUE_TOKEN_ENDPOINT = "/gateway/rm/opaqueToken";
}
