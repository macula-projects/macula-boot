/*
 * Copyright (c) 2023 Macula
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

package dev.macula.boot.starter.operationlog;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 *
 * @author Gordian
 * @since 2025-11-19
 */
@Slf4j
@Aspect
public class OperationLogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final String applicationName;

    public OperationLogAspect(ApplicationEventPublisher eventPublisher, String applicationName) {
        this.eventPublisher = eventPublisher;
        this.applicationName = applicationName;
    }

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        OperationLogDTO operationLogDTO = OperationLogUtils.getOperationLog(joinPoint, operationLog);
        operationLogDTO.setApplication(applicationName);
        operationLogDTO.setStartTime(LocalDateTime.now());

        Object result;
        try {
            result = joinPoint.proceed();
            if (operationLog.logResult()) {
                operationLogDTO.setResult(result);
            }
        } catch (Exception exception) {
            operationLogDTO.setLevel(OperationLogLevel.ERROR);
            operationLogDTO.setException(exception.getMessage());
            throw exception;
        } finally {
            operationLogDTO.setEndTime(LocalDateTime.now());
            operationLogDTO.setExecutionTimeMillis(
                Duration.between(operationLogDTO.getStartTime(), operationLogDTO.getEndTime()).toMillis()
            );
            eventPublisher.publishEvent(new OperationLogEvent(operationLogDTO));
        }

        return result;
    }

}