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
package dev.macula.boot.starter.tinyid.base.exception;

import dev.macula.boot.exception.BizException;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 TinyID 异常继承统一业务异常并保留结果码、详情和原始原因。
 *
 * @author Rain
 * @since 6.1.0
 */
class TinyIdSysExceptionTest {

    @Test
    void preservesLegacyConstructorsWithSystemErrorCode() {
        Throwable cause = new IllegalStateException("segment unavailable");
        TinyIdSysException empty = new TinyIdSysException();
        TinyIdSysException message = new TinyIdSysException("details");
        TinyIdSysException withCause = new TinyIdSysException("details", cause);
        TinyIdSysException causeOnly = new TinyIdSysException(cause);
        TinyIdSysException nullCause = new TinyIdSysException((Throwable) null);
        for (TinyIdSysException exception : new TinyIdSysException[] {
            empty, message, withCause, causeOnly, nullCause
        }) {
            assertThat(exception).isInstanceOf(BizException.class);
            assertThat(exception.getCode()).isEqualTo(ApiResultCode.SYS_ERROR.getCode());
            assertThat(exception.getMsg()).isEqualTo(ApiResultCode.SYS_ERROR.getMsg());
        }
        assertThat(empty).hasMessage(null).hasNoCause();
        assertThat(message).hasMessage("details").hasNoCause();
        assertThat(withCause).hasMessage("details").hasCause(cause);
        assertThat(causeOnly).hasMessage(cause.toString()).hasCause(cause);
        assertThat(nullCause).hasMessage(null).hasNoCause();
    }

    @Test
    void preservesCustomResultCodeSeparatelyFromDetailsAndCause() {
        ResultCode code = mock(ResultCode.class);
        when(code.getCode()).thenReturn("TINYID_TEST_ERROR");
        when(code.getMsg()).thenReturn("号段申请失败");
        Throwable cause = new IllegalStateException("unavailable");
        TinyIdSysException exception = new TinyIdSysException(code, "segment details", cause);
        assertThat(exception.getCode()).isEqualTo("TINYID_TEST_ERROR");
        assertThat(exception.getMsg()).isEqualTo("号段申请失败");
        assertThat(exception).hasMessage("segment details").hasCause(cause);

        TinyIdSysException withoutCause = new TinyIdSysException(code, "details");
        assertThat(withoutCause.getCode()).isEqualTo(exception.getCode());
        assertThat(withoutCause.getMsg()).isEqualTo(exception.getMsg());
        assertThat(withoutCause).hasMessage("details").hasNoCause();
    }
}
