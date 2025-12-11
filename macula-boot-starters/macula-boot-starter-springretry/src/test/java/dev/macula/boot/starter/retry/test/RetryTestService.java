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
    int a = 0;

    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void testRetry() {
        a++;
        System.out.println(a + " - 调用时间是" + LocalTime.now());
        if (a < 10) {
            throw new RuntimeException("未满足条件");
        }
    }

    @Recover
    public String recoverTest(RuntimeException e) {
        System.out.println("回调方法调用时间是-" + LocalTime.now() + "，异常信息-" + e.getMessage());
        return "回调方法-" + e.getMessage();
    }
}
