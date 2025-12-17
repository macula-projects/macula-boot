/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.starter.retry.test;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * {@code RetryTestService} 重试测试服务
 *
 * @author rain
 * @since 2024/1/25 14:59
 */
@Component
public class RetryTestService {
    int retryCount = 0;

    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String testRetry() {
        retryCount++;
        System.out.println("调用不稳定服务，第" + retryCount + "次");

        // 模拟业务异常（前2次失败，第3次成功）
        if (retryCount < 4) {
            throw new RuntimeException("服务临时不可用");
        }

        return "调用成功！";
    }

    @Recover
    public String recoverTest(RuntimeException e) {
        System.out.println("所有重试均失败，执行兜底逻辑：" + e.getMessage());
        return "兜底返回：服务暂时无法访问，请稍后重试";
    }
}
