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

package dev.macula.boot.starter.task.compat.xxljob;

import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.job.core.dto.ShardingJobArgs;
import com.aizuda.snailjob.common.core.util.JsonUtil;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import com.xxl.job.core.context.XxlJobContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invokes one XXL-compatible bean handler from a SnailJob execution.
 *
 * @author Rain
 * @since 6.1.0
 */
public final class XxlJobSnailJobExecutor {

    private final Object target;
    private final Method executeMethod;
    private final Method initMethod;
    private final Method destroyMethod;

    XxlJobSnailJobExecutor(Object target, Method executeMethod, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.executeMethod = executeMethod;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    /**
     * Executes the wrapped handler with an XXL-compatible thread context.
     *
     * @param jobArgs SnailJob execution arguments
     * @return SnailJob execution result
     */
    public ExecuteResult execute(JobArgs jobArgs) {
        XxlJobContext context = createContext(jobArgs);
        XxlJobContext.setXxlJobContext(context);
        try {
            executeMethod.invoke(target, new Object[executeMethod.getParameterCount()]);
            if (context.getHandleCode() == XxlJobContext.HANDLE_CODE_SUCCESS) {
                return context.getHandleMsg() == null ? ExecuteResult.success()
                    : ExecuteResult.success(context.getHandleMsg());
            }
            String message = context.getHandleMsg() == null ? "XXL-compatible task execution failed"
                : context.getHandleMsg();
            return ExecuteResult.failure(null, message);
        } catch (InvocationTargetException exception) {
            return failure(exception.getTargetException());
        } catch (ReflectiveOperationException exception) {
            return failure(exception);
        } finally {
            XxlJobContext.setXxlJobContext(null);
        }
    }

    void initialize() {
        invokeLifecycle(initMethod, "initialize");
    }

    void destroy() {
        invokeLifecycle(destroyMethod, "destroy");
    }

    private XxlJobContext createContext(JobArgs jobArgs) {
        String jobParam = serializeJobParam(jobArgs.getJobParams());
        long jobId = jobArgs.getJobId() == null ? -1 : jobArgs.getJobId();
        long logId = jobArgs.getTaskBatchId() == null ? -1 : jobArgs.getTaskBatchId();
        int shardIndex = 0;
        int shardTotal = 1;
        if (jobArgs instanceof ShardingJobArgs shardingJobArgs) {
            shardIndex = shardingJobArgs.getShardingIndex() == null ? 0 : shardingJobArgs.getShardingIndex();
            shardTotal = shardingJobArgs.getShardingTotal() == null ? 1 : shardingJobArgs.getShardingTotal();
        }
        return new XxlJobContext(jobId, jobParam, logId, System.currentTimeMillis(), null, shardIndex, shardTotal);
    }

    private String serializeJobParam(Object jobParam) {
        if (jobParam == null || jobParam instanceof String) {
            return (String) jobParam;
        }
        return JsonUtil.toJsonString(jobParam);
    }

    private void invokeLifecycle(Method method, String action) {
        if (method == null) {
            return;
        }
        try {
            method.invoke(target);
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Failed to " + action + " XXL-compatible task handler",
                exception.getTargetException());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to " + action + " XXL-compatible task handler", exception);
        }
    }

    private ExecuteResult failure(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getName();
        }
        return ExecuteResult.failure(null, message);
    }
}
