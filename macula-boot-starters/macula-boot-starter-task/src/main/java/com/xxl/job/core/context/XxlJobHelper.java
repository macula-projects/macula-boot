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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the common XXL handler helper API on top of a SnailJob execution context.
 *
 * @author Rain
 * @since 6.1.0
 */
public final class XxlJobHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("xxl-job logger");

    private XxlJobHelper() {
    }

    public static long getJobId() {
        XxlJobContext context = currentContext();
        return context == null ? -1 : context.getJobId();
    }

    public static String getJobParam() {
        XxlJobContext context = currentContext();
        return context == null ? null : context.getJobParam();
    }

    public static long getLogId() {
        XxlJobContext context = currentContext();
        return context == null ? -1 : context.getLogId();
    }

    public static long getLogDateTime() {
        XxlJobContext context = currentContext();
        return context == null ? -1 : context.getLogDateTime();
    }

    public static String getLogFileName() {
        XxlJobContext context = currentContext();
        return context == null ? null : context.getLogFileName();
    }

    public static int getShardIndex() {
        XxlJobContext context = currentContext();
        return context == null ? -1 : context.getShardIndex();
    }

    public static int getShardTotal() {
        XxlJobContext context = currentContext();
        return context == null ? -1 : context.getShardTotal();
    }

    /**
     * Writes an XXL-compatible handler message through SLF4J so the SnailJob appender can report it.
     *
     * @param pattern SLF4J message pattern
     * @param arguments message arguments
     * @return {@code true} when called inside a task context
     */
    public static boolean log(String pattern, Object... arguments) {
        LOGGER.info(pattern, arguments);
        return currentContext() != null;
    }

    /**
     * Writes an exception through SLF4J so the SnailJob appender can report it.
     *
     * @param throwable exception to report
     * @return {@code true} when called inside a task context
     */
    public static boolean log(Throwable throwable) {
        LOGGER.error("XXL-compatible task handler failed", throwable);
        return currentContext() != null;
    }

    public static boolean handleSuccess() {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, null);
    }

    public static boolean handleSuccess(String message) {
        return handleResult(XxlJobContext.HANDLE_CODE_SUCCESS, message);
    }

    public static boolean handleFail() {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, null);
    }

    public static boolean handleFail(String message) {
        return handleResult(XxlJobContext.HANDLE_CODE_FAIL, message);
    }

    public static boolean handleTimeout() {
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, null);
    }

    public static boolean handleTimeout(String message) {
        return handleResult(XxlJobContext.HANDLE_CODE_TIMEOUT, message);
    }

    /**
     * Updates the current execution result.
     *
     * @param handleCode XXL-compatible handle code
     * @param handleMsg optional result message
     * @return {@code false} when no task context is bound
     */
    public static boolean handleResult(int handleCode, String handleMsg) {
        XxlJobContext context = currentContext();
        if (context == null) {
            return false;
        }
        context.setHandleCode(handleCode);
        context.setHandleMsg(handleMsg);
        return true;
    }

    private static XxlJobContext currentContext() {
        return XxlJobContext.getXxlJobContext();
    }
}
