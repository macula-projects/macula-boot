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

    private final ApplicationEventPublisher publisher;
    private final String applicationName;

    public OperationLogAspect(ApplicationEventPublisher publisher, String applicationName) {
        this.publisher = publisher;
        this.applicationName = applicationName;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        String strClassName = point.getTarget().getClass().getName();
        String strMethodName = point.getSignature().getName();
        log.debug("[类名]:{},[方法]:{}", strClassName, strMethodName);

        //封装操作日志
        OperationLogDTO logDTO = OperationLogUtils.getOperationLog(point, operationLog);
        //服务名称
        logDTO.setServiceId(applicationName);
        //开始时间
        logDTO.setStartTime(LocalDateTime.now());
        Object obj;
        try {
            obj = point.proceed();
            //返回值
            if (operationLog.isShowResult()) {
                logDTO.setResult(obj);
            }
        } catch (Exception e) {
            //日志类型
            logDTO.setLogType(OperationLogTypeEnum.ERROR);
            //异常信息
            logDTO.setException(e.getMessage());
            throw e;
        } finally {
            //结束时间
            logDTO.setEndTime(LocalDateTime.now());
            //操作执行时间
            logDTO.setCostTime(Duration.between(logDTO.getStartTime(), logDTO.getEndTime()).toMillis());
            publisher.publishEvent(new OperationLogEvent(logDTO));
        }
        return obj;
    }

}