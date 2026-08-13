/*
 * Copyright (c) 2023-2026 Macula
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

package dev.macula.example.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.core.util.JsonUtil;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.springframework.stereotype.Component;

/**
 * {@code SnailJobHandler} is SnailJobHandler测试类
 *
 * @author Rain
 * @since 2025/12/10 15:33
 */
@Component
public class SnailJobHandler {
    @JobExecutor(name = "demoJobHandler")
    public ExecuteResult demoJobHandler(JobArgs jobArgs) {
        SnailJobLog.REMOTE.info("哈哈，测试成功了");
        System.out.println(JsonUtil.toJsonString(jobArgs));
        return ExecuteResult.success();
    }
}
