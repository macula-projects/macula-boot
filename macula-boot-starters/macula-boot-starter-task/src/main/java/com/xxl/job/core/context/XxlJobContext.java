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

/**
 * Holds the XXL-compatible task context backed by a SnailJob execution.
 *
 * @author Rain
 * @since 6.1.0
 */
public class XxlJobContext {

    public static final int HANDLE_CODE_SUCCESS = 200;
    public static final int HANDLE_CODE_FAIL = 500;
    public static final int HANDLE_CODE_TIMEOUT = 502;

    private static final InheritableThreadLocal<XxlJobContext> CONTEXT_HOLDER = new InheritableThreadLocal<>();

    private final long jobId;
    private final String jobParam;
    private final long logId;
    private final long logDateTime;
    private final String logFileName;
    private final int shardIndex;
    private final int shardTotal;
    private int handleCode = HANDLE_CODE_SUCCESS;
    private String handleMsg;

    /**
     * Creates an execution context compatible with commonly used XXL handler APIs.
     *
     * @param jobId task identifier
     * @param jobParam serialized task parameter
     * @param logId execution batch identifier
     * @param logDateTime execution start time in milliseconds
     * @param logFileName local log file name, normally {@code null} for SnailJob
     * @param shardIndex zero-based shard index
     * @param shardTotal total number of shards
     */
    public XxlJobContext(long jobId, String jobParam, long logId, long logDateTime, String logFileName, int shardIndex,
        int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.logId = logId;
        this.logDateTime = logDateTime;
        this.logFileName = logFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public long getLogId() {
        return logId;
    }

    public long getLogDateTime() {
        return logDateTime;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    /**
     * Binds a context to the current execution thread. Passing {@code null} clears it.
     *
     * @param context execution context, or {@code null}
     */
    public static void setXxlJobContext(XxlJobContext context) {
        if (context == null) {
            CONTEXT_HOLDER.remove();
        } else {
            CONTEXT_HOLDER.set(context);
        }
    }

    /**
     * Returns the context bound to the current execution thread.
     *
     * @return current context, or {@code null}
     */
    public static XxlJobContext getXxlJobContext() {
        return CONTEXT_HOLDER.get();
    }
}
