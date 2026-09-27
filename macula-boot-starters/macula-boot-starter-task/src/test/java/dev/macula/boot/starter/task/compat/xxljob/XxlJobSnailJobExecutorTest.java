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
import com.aizuda.snailjob.common.core.enums.StatusEnum;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests parameter, context and result mapping performed by {@link XxlJobSnailJobExecutor}.
 *
 * @author Rain
 * @since 6.1.0
 */
class XxlJobSnailJobExecutorTest {

    @AfterEach
    void clearContext() {
        XxlJobContext.setXxlJobContext(null);
    }

    @Test
    void mapsStringAndIdentifiersAndClearsContext() throws Exception {
        CapturingHandler handler = new CapturingHandler();
        JobArgs args = new JobArgs();
        args.setJobId(12L);
        args.setTaskBatchId(34L);
        args.setJobParams("hello");

        ExecuteResult result = executor(handler, "capture").execute(args);

        assertThat(result.getStatus()).isEqualTo(StatusEnum.YES.getStatus());
        assertThat(handler.jobId).isEqualTo(12L);
        assertThat(handler.logId).isEqualTo(34L);
        assertThat(handler.jobParam).isEqualTo("hello");
        assertThat(handler.shardIndex).isZero();
        assertThat(handler.shardTotal).isEqualTo(1);
        assertThat(XxlJobContext.getXxlJobContext()).isNull();
    }

    @Test
    void serializesObjectParameterAndMapsSharding() throws Exception {
        CapturingHandler handler = new CapturingHandler();
        ShardingJobArgs args = new ShardingJobArgs();
        args.setJobParams(Map.of("name", "macula"));
        args.setShardingIndex(2);
        args.setShardingTotal(5);

        executor(handler, "capture").execute(args);

        assertThat(handler.jobParam).contains("\"name\":\"macula\"");
        assertThat(handler.shardIndex).isEqualTo(2);
        assertThat(handler.shardTotal).isEqualTo(5);
    }

    @Test
    void preservesEmptyParameter() throws Exception {
        CapturingHandler handler = new CapturingHandler();

        executor(handler, "capture").execute(new JobArgs());

        assertThat(handler.jobParam).isNull();
    }

    @Test
    void passesNullToReferenceParameters() throws Exception {
        CapturingHandler handler = new CapturingHandler();

        executor(handler, "reference", String.class).execute(new JobArgs());

        assertThat(handler.reference).isNull();
    }

    @Test
    void mapsExplicitFailureAndTimeout() throws Exception {
        ExecuteResult failure = executor(new ResultHandler(), "fail").execute(new JobArgs());
        ExecuteResult timeout = executor(new ResultHandler(), "timeout").execute(new JobArgs());

        assertThat(failure.getStatus()).isEqualTo(StatusEnum.NO.getStatus());
        assertThat(failure.getMessage()).isEqualTo("failed");
        assertThat(timeout.getStatus()).isEqualTo(StatusEnum.NO.getStatus());
        assertThat(timeout.getMessage()).isEqualTo("timed out");
    }

    @Test
    void mapsExplicitSuccessMessage() throws Exception {
        ExecuteResult success = executor(new ResultHandler(), "success").execute(new JobArgs());

        assertThat(success.getStatus()).isEqualTo(StatusEnum.YES.getStatus());
        assertThat(success.getResult()).isEqualTo("done");
    }

    @Test
    void convertsBusinessExceptionToFailureAndClearsContext() throws Exception {
        ExecuteResult result = executor(new ResultHandler(), "explode").execute(new JobArgs());

        assertThat(result.getStatus()).isEqualTo(StatusEnum.NO.getStatus());
        assertThat(result.getMessage()).isEqualTo("boom");
        assertThat(XxlJobContext.getXxlJobContext()).isNull();
    }

    private XxlJobSnailJobExecutor executor(Object target, String methodName, Class<?>... parameterTypes)
        throws NoSuchMethodException {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return new XxlJobSnailJobExecutor(target, method, null, null);
    }

    /**
     * Captures values exposed through the compatibility helper.
     *
     * @since 6.1.0
     */
    static class CapturingHandler {

        private long jobId;
        private long logId;
        private String jobParam;
        private int shardIndex;
        private int shardTotal;
        private String reference = "not-null";

        void capture() {
            jobId = XxlJobHelper.getJobId();
            logId = XxlJobHelper.getLogId();
            jobParam = XxlJobHelper.getJobParam();
            shardIndex = XxlJobHelper.getShardIndex();
            shardTotal = XxlJobHelper.getShardTotal();
        }

        void reference(String value) {
            reference = value;
        }
    }

    /**
     * Produces explicit and exceptional handler outcomes.
     *
     * @since 6.1.0
     */
    static class ResultHandler {

        void success() {
            XxlJobHelper.handleSuccess("done");
        }

        void fail() {
            XxlJobHelper.handleFail("failed");
        }

        void timeout() {
            XxlJobHelper.handleTimeout("timed out");
        }

        void explode() {
            throw new IllegalArgumentException("boom");
        }
    }
}
