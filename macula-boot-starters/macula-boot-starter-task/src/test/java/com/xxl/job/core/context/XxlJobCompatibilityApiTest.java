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

package com.xxl.job.core.context;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the minimal XXL-compatible API used by existing bean handlers.
 *
 * @author Rain
 * @since 6.1.0
 */
class XxlJobCompatibilityApiTest {

    @AfterEach
    void clearContext() {
        XxlJobContext.setXxlJobContext(null);
    }

    @Test
    void exposesAnnotationContract() throws Exception {
        Method method = AnnotatedHandler.class.getDeclaredMethod("execute");
        XxlJob annotation = method.getAnnotation(XxlJob.class);

        assertThat(annotation.value()).isEqualTo("compatibilityHandler");
        assertThat(annotation.init()).isEqualTo("init");
        assertThat(annotation.destroy()).isEqualTo("destroy");
    }

    @Test
    void exposesStableDefaultsWithoutContext() {
        assertThat(XxlJobHelper.getJobId()).isEqualTo(-1L);
        assertThat(XxlJobHelper.getJobParam()).isNull();
        assertThat(XxlJobHelper.getLogId()).isEqualTo(-1L);
        assertThat(XxlJobHelper.getShardIndex()).isEqualTo(-1);
        assertThat(XxlJobHelper.getShardTotal()).isEqualTo(-1);
        assertThat(XxlJobHelper.handleSuccess()).isFalse();
        assertThat(XxlJobHelper.log("outside context {}", "value")).isFalse();
    }

    @Test
    void updatesBoundContextThroughHelper() {
        XxlJobContext context = new XxlJobContext(1L, "param", 2L, 3L, null, 4, 5);
        XxlJobContext.setXxlJobContext(context);

        assertThat(XxlJobHelper.handleResult(XxlJobContext.HANDLE_CODE_FAIL, "failed")).isTrue();
        assertThat(context.getHandleCode()).isEqualTo(XxlJobContext.HANDLE_CODE_FAIL);
        assertThat(context.getHandleMsg()).isEqualTo("failed");
        assertThat(XxlJobHelper.log(new IllegalStateException("loggable"))).isTrue();
    }

    /**
     * Represents unchanged legacy annotation usage.
     *
     * @since 6.1.0
     */
    static class AnnotatedHandler {

        @XxlJob(value = "compatibilityHandler", init = "init", destroy = "destroy")
        void execute() {
        }
    }
}
