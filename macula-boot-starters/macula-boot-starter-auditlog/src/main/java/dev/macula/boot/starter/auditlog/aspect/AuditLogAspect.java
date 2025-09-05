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

package dev.macula.boot.starter.auditlog.aspect;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import dev.macula.boot.starter.auditlog.annotation.AuditLog;
import dev.macula.boot.starter.auditlog.enums.BusinessStatus;
import dev.macula.boot.starter.auditlog.event.OperLogEvent;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 操作日志记录处理
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
@AutoConfiguration
public class AuditLogAspect implements ApplicationContextAware {
    /** 排除敏感属性字段 */
    public static final String[] EXCLUDE_PROPERTIES = {"password", "oldPassword", "newPassword", "confirmPassword"};
    private static final String LOCAL_HOST = "127.0.0.1";
    private static final String LOCAL_HOST_IPV6 = "0:0:0:0:0:0:0:1";
    private ApplicationContext applicationContext;

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, AuditLog controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, AuditLog controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, AuditLog controllerLog, final Exception e, Object jsonResult) {
        try {
            HttpServletRequest request =
                ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();

            if (request != null) {
                // *========数据库日志=========*//
                OperLogEvent operLog = new OperLogEvent();
                operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
                // 请求的地址
                String ip = ServletUtil.getClientIP(request);
                if (LOCAL_HOST_IPV6.equals(ip)) {
                    ip = LOCAL_HOST;
                }
                operLog.setOperIp(ip);
                operLog.setOperUrl(StrUtil.sub(request.getRequestURI(), 0, 255));
                operLog.setOperName(SecurityUtils.getCurrentUser());
                operLog.setOperTime(new Date());

                if (e != null) {
                    operLog.setStatus(BusinessStatus.FAIL.ordinal());
                    operLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
                }
                // 设置方法名称
                String className = joinPoint.getTarget().getClass().getName();
                String methodName = joinPoint.getSignature().getName();
                operLog.setMethod(className + "." + methodName + "()");
                // 设置请求方式
                operLog.setRequestMethod(request.getMethod());
                // 处理设置注解上的参数
                getControllerMethodDescription(joinPoint, request, controllerLog, operLog, jsonResult);
                // 发布事件保存数据库
                applicationContext.publishEvent(operLog);
            }
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param log     日志
     * @param operLog 操作日志
     * @throws Exception
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, HttpServletRequest request, AuditLog log,
        OperLogEvent operLog, Object jsonResult) throws Exception {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, request, operLog, log);
        }
        // 是否需要保存response，参数和值
        if (log.isSaveResponseData() && ObjectUtil.isNotNull(jsonResult)) {
            if (ObjectUtil.isBasicType(jsonResult)) {
                operLog.setJsonResult(jsonResult.toString());
            } else {
                operLog.setJsonResult(StrUtil.sub(JSONUtil.toJsonStr(jsonResult), 0, log.maxResultLength()));
            }
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param operLog 操作日志
     */
    private void setRequestValue(JoinPoint joinPoint, HttpServletRequest request, OperLogEvent operLog, AuditLog log) {
        Map<String, String> paramsMap = ServletUtil.getParamMap(request);
        String requestMethod = operLog.getRequestMethod();
        if (MapUtil.isEmpty(paramsMap) && HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod)) {
            String params = argsArrayToString(joinPoint.getArgs(), log.excludeParamNames());
            operLog.setOperParam(StrUtil.sub(params, 0, log.maxParamLength()));
        } else {
            MapUtil.removeAny(paramsMap, EXCLUDE_PROPERTIES);
            MapUtil.removeAny(paramsMap, log.excludeParamNames());
            operLog.setOperParam(StrUtil.sub(JSONUtil.toJsonStr(paramsMap), 0, log.maxParamLength()));
        }
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames) {
        StringJoiner params = new StringJoiner(" ");
        if (ArrayUtil.isEmpty(paramsArray)) {
            return params.toString();
        }

        int i = 0;
        for (Object o : paramsArray) {
            if (ObjectUtil.isNotNull(o) && !isFilterObject(o)) {
                String str = JSONUtil.toJsonStr(o);
                if (!o.getClass().isPrimitive() && !Collection.class.isAssignableFrom(o.getClass()) && !o.getClass()
                    .isArray()) {
                    Dict dict = JSONUtil.toBean(str, Dict.class);
                    if (MapUtil.isNotEmpty(dict)) {
                        MapUtil.removeAny(dict, EXCLUDE_PROPERTIES);
                        MapUtil.removeAny(dict, excludeParamNames);
                        str = JSONUtil.toJsonStr(dict);
                    }
                }
                params.add(str);
            }
        }
        return params.toString();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection)o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map)o;
            for (Object value : map.values()) {
                return value instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse || o instanceof BindingResult;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
