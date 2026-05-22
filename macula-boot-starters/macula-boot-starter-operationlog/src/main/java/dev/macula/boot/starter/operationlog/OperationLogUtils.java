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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.Principal;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 操作日志工具类
 *
 * @author Gordian
 * @since 2025-11-19
 */
@Slf4j
public class OperationLogUtils {

    private static final int MAX_JSON_LENGTH = 4000;

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL);

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

    private static JsonNode extractParameters(ProceedingJoinPoint joinPoint) {
        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();

        Map<String, Object> parameters = new LinkedHashMap<>(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            parameters.put(parameterNames[i], filterValue(parameterValues[i]));
        }
        return safeToJson(parameters);
    }

    /**
     * 安全地将对象转换为JsonNode，循环引用自动写为null
     *
     * @param obj 要转换的对象
     * @return JsonNode
     */
    public static JsonNode safeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (isNonSerializable(obj)) {
            return TextNode.valueOf(obj.getClass().getSimpleName());
        }
        try {
            JsonNode node = MAPPER.valueToTree(obj);
            String str = node.toString();
            if (str.length() <= MAX_JSON_LENGTH) {
                return node;
            }
            log.debug("JSON truncated from {} to {} characters", str.length(), MAX_JSON_LENGTH);
            return TextNode.valueOf(str.substring(0, MAX_JSON_LENGTH) + "...(truncated)");
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            try {
                return TextNode.valueOf(obj.toString());
            } catch (Exception ex) {
                return TextNode.valueOf(obj.getClass().getName() + "(toString failed)");
            }
        }
    }

    private static Object filterValue(Object value) {
        if (!isNonSerializable(value)) {
            return value;
        }
        return value.getClass().getSimpleName();
    }

    private static boolean isNonSerializable(Object obj) {
        // Servlet API
        if (obj instanceof HttpServletRequest || obj instanceof ServletResponse || obj instanceof HttpSession) {
            return true;
        }
        // Spring MVC
        if (obj instanceof MultipartFile) {
            return true;
        }
        // I/O
        if (obj instanceof InputStream || obj instanceof OutputStream
            || obj instanceof Reader || obj instanceof Writer) {
            return true;
        }
        // JDK 基础设施
        if (obj instanceof Class || obj instanceof ClassLoader || obj instanceof Thread
            || obj instanceof Principal || obj instanceof ExecutorService) {
            return true;
        }
        // JDBC
        //noinspection RedundantIfStatement
        if (obj instanceof Connection || obj instanceof DataSource) {
            return true;
        }
        return false;
    }

}
