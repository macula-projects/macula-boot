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

import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 操作日志工具类
 *
 * @author Gordian
 * @since 2025-11-19
 */
public class OperationLogUtils {

    public static OperationLogDTO getOperationLog(ProceedingJoinPoint joinPoint, OperationLog operationLog) {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) Objects
            .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        OperationLogDTO operationLogDTO = new OperationLogDTO();
        operationLogDTO.setLevel(OperationLogLevel.INFO);
        operationLogDTO.setClientIp(ServletUtil.getClientIP(httpRequest));

        if (OperationLogConstant.SCOPE_CONTROLLER.equals(operationLog.scope())) {
            operationLogDTO.setRequestUri(URLUtil.getPath(httpRequest.getRequestURI()));
            operationLogDTO.setRequestMethod(httpRequest.getMethod());
        }

        if (operationLog.logParameters()) {
            operationLogDTO.setParameters(extractParameters(joinPoint));
        }

        String targetClassName = joinPoint.getTarget().getClass().getSimpleName();
        String targetMethodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        operationLogDTO.setMethod(targetClassName + "." + targetMethodName);
        operationLogDTO.setDescription(operationLog.description());
        operationLogDTO.setOperation(operationLog.operation());
        operationLogDTO.setModule(operationLog.module());
        operationLogDTO.setScope(operationLog.scope());

        return operationLogDTO;
    }

    private static Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();

        Map<String, Object> parameters = new HashMap<>(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            parameters.put(parameterNames[i], parameterValues[i]);
        }
        return parameters;
    }

}