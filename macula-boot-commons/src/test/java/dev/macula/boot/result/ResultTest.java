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
package dev.macula.boot.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@code ResultTest} 统一响应结果单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class ResultTest {

    @Test
    void shouldCreateDefaultSuccessResult() {
        Result<Object> result = Result.success();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo(ApiResultCode.SUCCESS.getCode());
        assertThat(result.getMsg()).isEqualTo(ApiResultCode.SUCCESS.getMsg());
        assertThat(result.getData()).isNull();
        assertThat(result.getCause()).isNull();
    }

    @Test
    void shouldCreateSuccessResultWithDataAndCustomMessage() {
        Result<String> result = Result.success("payload", "处理完成");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo(ApiResultCode.SUCCESS.getCode());
        assertThat(result.getMsg()).isEqualTo("处理完成");
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    void shouldCreateFailureResultFromResultCodeAndCause() {
        Result<Object> result = Result.failed(ApiResultCode.VALIDATE_ERROR, "name不能为空");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ApiResultCode.VALIDATE_ERROR.getCode());
        assertThat(result.getMsg()).isEqualTo(ApiResultCode.VALIDATE_ERROR.getMsg());
        assertThat(result.getCause()).isEqualTo("name不能为空");
        assertThat(result.getData()).isNull();
    }

    @Test
    void shouldCreateFailureResultWithCustomCode() {
        Result<Object> result = Result.failed("B1001", "库存不足", "sku=100");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo("B1001");
        assertThat(result.getMsg()).isEqualTo("库存不足");
        assertThat(result.getCause()).isEqualTo("sku=100");
    }

    @Test
    void shouldJudgeResultFromBooleanStatus() {
        assertThat(Result.judge(true).isSuccess()).isTrue();
        assertThat(Result.judge(false).isSuccess()).isFalse();
        assertThat(Result.judge(false).getCode()).isEqualTo(ApiResultCode.FAILED.getCode());
    }

    @Test
    void shouldExposeStableApiResultCodeRepresentation() {
        assertThat(ApiResultCode.SUCCESS.toString()).isEqualTo("code=00000, msg=请求成功");
    }
}
