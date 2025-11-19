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

    /**
     * 获取操作日志信息
     *
     * @param point        切点
     * @param operationLog 操作日志注解
     * @return 操作日志DTO
     */
    public static OperationLogDTO getOperationLog(ProceedingJoinPoint point, OperationLog operationLog) {
        HttpServletRequest request = ((ServletRequestAttributes) Objects
            .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        OperationLogDTO logDTO = new OperationLogDTO();
        logDTO.setLogType(OperationLogTypeEnum.NORMAL);
        logDTO.setClientIp(ServletUtil.getClientIP(request));
        //如果是http接口请求，则多打印http请求信息
        if (OperationLogConstant.LAYER_CONTROLLER.equals(operationLog.layer())) {
            logDTO.setRequestUri(URLUtil.getPath(request.getRequestURI()));
            logDTO.setRequestMode(request.getMethod());
        }
        if (operationLog.logParameters()) {
            logDTO.setParam(new OperationLogUtils().getParams(point));
        }
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = ((MethodSignature) point.getSignature()).getMethod().getName();
        logDTO.setMethod(className + "." + methodName);
        //操作说明
        logDTO.setDescription(operationLog.description());
        //操作类型
        logDTO.setOperation(operationLog.operation());
        //模块名称
        logDTO.setModule(operationLog.module());
        //业务层级
        logDTO.setLayer(operationLog.layer());
        return logDTO;
    }

    /**
     * 获取方法参数
     *
     * @param point 切点
     * @return 参数Map
     */
    private Map<String, Object> getParams(ProceedingJoinPoint point) {
        Map<String, Object> map = new HashMap<>(16);
        Object[] values = point.getArgs();
        String[] names = ((MethodSignature) point.getSignature()).getParameterNames();
        for (int i = 0; i < names.length; i++) {
            map.put(names[i], values[i]);
        }
        return map;
    }

}