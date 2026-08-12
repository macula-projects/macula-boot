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
package dev.macula.boot.exception;

import static org.assertj.core.api.Assertions.assertThat;

import dev.macula.boot.result.ApiResultCode;
import org.junit.jupiter.api.Test;

/**
 * {@code BusinessExceptionTest} 业务异常单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class BusinessExceptionTest {

    @Test
    void shouldCreateBizExceptionWithDefaultResultCode() {
        BizException exception = new BizException("订单金额不能小于零");

        assertThat(exception.getCode()).isEqualTo(ApiResultCode.BIZ_ERROR.getCode());
        assertThat(exception.getMsg()).isEqualTo(ApiResultCode.BIZ_ERROR.getMsg());
        assertThat(exception).hasMessage("订单金额不能小于零");
    }

    @Test
    void shouldCreateBizExceptionWithSpecifiedResultCode() {
        BizException exception = new BizException(ApiResultCode.VALIDATE_ERROR, "缺少必填参数");

        assertThat(exception.getCode()).isEqualTo(ApiResultCode.VALIDATE_ERROR.getCode());
        assertThat(exception.getMsg()).isEqualTo(ApiResultCode.VALIDATE_ERROR.getMsg());
        assertThat(exception).hasMessage("缺少必填参数");
    }

    @Test
    void shouldCreateBizCheckExceptionWithDefaultResultCode() {
        BizCheckException exception = new BizCheckException("库存不足");

        assertThat(exception.getCode()).isEqualTo(ApiResultCode.BIZ_CHECK_ERROR.getCode());
        assertThat(exception.getMsg()).isEqualTo(ApiResultCode.BIZ_CHECK_ERROR.getMsg());
        assertThat(exception).hasMessage("库存不足");
    }

    @Test
    void shouldRetainMaculaExceptionCause() {
        IllegalStateException cause = new IllegalStateException("root cause");
        MaculaException exception = new MaculaException("framework error", cause);

        assertThat(exception).hasMessage("framework error").hasCause(cause);
    }
}
