/*
 * Copyright 2004-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maculaframework.boot.core.cache.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.maculaframework.boot.core.cache.annotation.*;
import org.maculaframework.boot.core.cache.cache.Cache;
import org.maculaframework.boot.core.cache.expression.CacheOperationExpressionEvaluator;
import org.maculaframework.boot.core.cache.manager.CacheManager;
import org.maculaframework.boot.core.cache.setting.FirstCacheSetting;
import org.maculaframework.boot.core.cache.setting.LayeringCacheSetting;
import org.maculaframework.boot.core.cache.setting.SecondaryCacheSetting;
import org.maculaframework.boot.core.cache.support.CacheOperationInvoker;
import org.maculaframework.boot.core.cache.support.KeyGenerator;
import org.maculaframework.boot.core.cache.support.SerializationException;
import org.maculaframework.boot.core.cache.support.SimpleKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;

/**
 * ???????????????????????????????????????
 *
 * @author yuhao.wang
 */
@Aspect
public class LayeringAspect {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CACHE_KEY_ERROR_MESSAGE = "??????Key %s ?????????NULL";
    private static final String CACHE_NAME_ERROR_MESSAGE = "?????????????????????NULL";

    /**
     * SpEL??????????????????
     */
    private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

    @Autowired
    private CacheManager cacheManager;

    @Autowired(required = false)
    private KeyGenerator keyGenerator = new SimpleKeyGenerator();

    @Pointcut("@annotation(org.maculaframework.boot.core.cache.annotation.Cacheable)")
    public void cacheablePointcut() {
    }

    @Pointcut("@annotation(org.maculaframework.boot.core.cache.annotation.CacheEvict)")
    public void cacheEvictPointcut() {
    }

    @Pointcut("@annotation(org.maculaframework.boot.core.cache.annotation.CachePut)")
    public void cachePutPointcut() {
    }

    @Around("cacheablePointcut()")
    public Object cacheablePointcut(ProceedingJoinPoint joinPoint) throws Throwable {
        CacheOperationInvoker aopAllianceInvoker = getCacheOperationInvoker(joinPoint);

        // ??????method
        Method method = this.getSpecificmethod(joinPoint);
        // ????????????
        Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);

        try {
            // ????????????????????????
            return executeCacheable(aopAllianceInvoker, cacheable, method, joinPoint.getArgs(), joinPoint.getTarget());
        } catch (SerializationException e) {
            // ???????????????????????????????????????????????????
            String[] cacheNames = cacheable.cacheNames();
            // ????????????
            delete(cacheNames, cacheable.key(), method, joinPoint.getArgs(), joinPoint.getTarget());

            // ??????????????????????????????????????????
            if (cacheable.ignoreException()) {
                logger.warn(e.getMessage(), e);
                return aopAllianceInvoker.invoke();
            }
            throw e;
        } catch (Exception e) {
            // ??????????????????????????????????????????
            if (cacheable.ignoreException()) {
                logger.warn(e.getMessage(), e);
                return aopAllianceInvoker.invoke();
            }
            throw e;
        }
    }

    @Around("cacheEvictPointcut()")
    public Object cacheEvictPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
        CacheOperationInvoker aopAllianceInvoker = getCacheOperationInvoker(joinPoint);

        // ??????method
        Method method = this.getSpecificmethod(joinPoint);
        // ????????????
        CacheEvict cacheEvict = AnnotationUtils.findAnnotation(method, CacheEvict.class);

        try {
            // ????????????????????????
            return executeEvict(aopAllianceInvoker, cacheEvict, method, joinPoint.getArgs(), joinPoint.getTarget());
        } catch (Exception e) {
            // ??????????????????????????????????????????
            if (cacheEvict.ignoreException()) {
                logger.warn(e.getMessage(), e);
                return aopAllianceInvoker.invoke();
            }
            throw e;
        }
    }

    @Around("cachePutPointcut()")
    public Object cachePutPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
        CacheOperationInvoker aopAllianceInvoker = getCacheOperationInvoker(joinPoint);

        // ??????method
        Method method = this.getSpecificmethod(joinPoint);
        // ????????????
        CachePut cacheEvict = AnnotationUtils.findAnnotation(method, CachePut.class);

        try {
            // ????????????????????????
            return executePut(aopAllianceInvoker, cacheEvict, method, joinPoint.getArgs(), joinPoint.getTarget());
        } catch (Exception e) {
            // ??????????????????????????????????????????
            if (cacheEvict.ignoreException()) {
                logger.warn(e.getMessage(), e);
                return aopAllianceInvoker.invoke();
            }
            throw e;
        }
    }

    /**
     * ??????Cacheable??????
     *
     * @param invoker   ???????????????????????????
     * @param cacheable {@link Cacheable}
     * @param method    {@link Method}
     * @param args      ??????????????????
     * @param target    target
     * @return {@link Object}
     */
    private Object executeCacheable(CacheOperationInvoker invoker, Cacheable cacheable,
                                    Method method, Object[] args, Object target) {

        // ??????SpEL???????????????cacheName???key
        String[] cacheNames = cacheable.cacheNames();
        Assert.notEmpty(cacheable.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
        String cacheName = cacheNames[0];
        Object key = generateKey(cacheable.key(), method, args, target);
        Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cacheable.key()));

        // ??????????????????????????????
        FirstCache firstCache = cacheable.firstCache();
        SecondaryCache secondaryCache = cacheable.secondaryCache();
        FirstCacheSetting firstCacheSetting = new FirstCacheSetting(firstCache.initialCapacity(), firstCache.maximumSize(),
                firstCache.expireTime(), firstCache.timeUnit(), firstCache.expireMode());

        SecondaryCacheSetting secondaryCacheSetting = new SecondaryCacheSetting(secondaryCache.expireTime(),
                secondaryCache.preloadTime(), secondaryCache.timeUnit(), secondaryCache.forceRefresh(),
                secondaryCache.isAllowNullValue(), secondaryCache.magnification());

        LayeringCacheSetting layeringCacheSetting = new LayeringCacheSetting(firstCacheSetting, secondaryCacheSetting,
                cacheable.depict());

        // ??????cacheName?????????????????????Cache
        Cache cache = cacheManager.getCache(cacheName, layeringCacheSetting);

        // ???Cache?????????
        return cache.get(key, () -> invoker.invoke());
    }

    /**
     * ?????? CacheEvict ??????
     *
     * @param invoker    ???????????????????????????
     * @param cacheEvict {@link CacheEvict}
     * @param method     {@link Method}
     * @param args       ??????????????????
     * @param target     target
     * @return {@link Object}
     */
    private Object executeEvict(CacheOperationInvoker invoker, CacheEvict cacheEvict,
                                Method method, Object[] args, Object target) {

        // ??????SpEL???????????????cacheName???key
        String[] cacheNames = cacheEvict.cacheNames();
        Assert.notEmpty(cacheEvict.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
        // ????????????????????????????????????
        if (cacheEvict.allEntries()) {
            // ????????????????????????????????????
            for (String cacheName : cacheNames) {
                Collection<Cache> caches = cacheManager.getCache(cacheName);
                if (CollectionUtils.isEmpty(caches)) {
                    // ??????????????????Cache????????????????????????
                    Cache cache = cacheManager.getCache(cacheName,
                            new LayeringCacheSetting(new FirstCacheSetting(), new SecondaryCacheSetting(), "???????????????????????????????????????"));
                    cache.clear();
                } else {
                    for (Cache cache : caches) {
                        cache.clear();
                    }
                }
            }
        } else {
            // ????????????key
            delete(cacheNames, cacheEvict.key(), method, args, target);
        }

        // ????????????
        return invoker.invoke();
    }

    /**
     * ????????????????????????????????????key
     *
     * @param cacheNames ????????????
     * @param keySpEL    key???SpEL?????????
     * @param method     {@link Method}
     * @param args       ????????????
     * @param target     ?????????
     */
    private void delete(String[] cacheNames, String keySpEL, Method method, Object[] args, Object target) {
        Object key = generateKey(keySpEL, method, args, target);
        Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, keySpEL));
        for (String cacheName : cacheNames) {
            Collection<Cache> caches = cacheManager.getCache(cacheName);
            if (CollectionUtils.isEmpty(caches)) {
                // ??????????????????Cache????????????????????????
                Cache cache = cacheManager.getCache(cacheName,
                        new LayeringCacheSetting(new FirstCacheSetting(), new SecondaryCacheSetting(), "???????????????????????????????????????"));
                cache.evict(key);
            } else {
                for (Cache cache : caches) {
                    cache.evict(key);
                }
            }
        }
    }

    /**
     * ?????? CachePut ??????
     *
     * @param invoker  ???????????????????????????
     * @param cachePut {@link CachePut}
     * @param method   {@link Method}
     * @param args     ??????????????????
     * @param target   target
     * @return {@link Object}
     */
    private Object executePut(CacheOperationInvoker invoker, CachePut cachePut, Method method, Object[] args, Object target) {


        String[] cacheNames = cachePut.cacheNames();
        Assert.notEmpty(cachePut.cacheNames(), CACHE_NAME_ERROR_MESSAGE);
        // ??????SpEL??????????????? key
        Object key = generateKey(cachePut.key(), method, args, target);
        Assert.notNull(key, String.format(CACHE_KEY_ERROR_MESSAGE, cachePut.key()));

        // ??????????????????????????????
        FirstCache firstCache = cachePut.firstCache();
        SecondaryCache secondaryCache = cachePut.secondaryCache();
        FirstCacheSetting firstCacheSetting = new FirstCacheSetting(firstCache.initialCapacity(), firstCache.maximumSize(),
                firstCache.expireTime(), firstCache.timeUnit(), firstCache.expireMode());

        SecondaryCacheSetting secondaryCacheSetting = new SecondaryCacheSetting(secondaryCache.expireTime(),
                secondaryCache.preloadTime(), secondaryCache.timeUnit(), secondaryCache.forceRefresh(),
                secondaryCache.isAllowNullValue(), secondaryCache.magnification());

        LayeringCacheSetting layeringCacheSetting = new LayeringCacheSetting(firstCacheSetting, secondaryCacheSetting,
                cachePut.depict());

        // ?????????????????????????????????
        Object result = invoker.invoke();

        for (String cacheName : cacheNames) {
            // ??????cacheName?????????????????????Cache
            Cache cache = cacheManager.getCache(cacheName, layeringCacheSetting);
            cache.put(key, result);
        }

        return result;
    }

    private CacheOperationInvoker getCacheOperationInvoker(ProceedingJoinPoint joinPoint) {
        return () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable ex) {
                throw new CacheOperationInvoker.ThrowableWrapperException(ex);
            }
        };
    }

    /**
     * ??????SpEL??????????????????????????????key?????????
     *
     * @return Object
     */
    private Object generateKey(String keySpEl, Method method, Object[] args, Object target) {

        // ??????????????????key?????????
        Class<?> targetClass = getTargetClass(target);
        if (StringUtils.hasText(keySpEl)) {
            EvaluationContext evaluationContext = evaluator.createEvaluationContext(method, args, target,
                    targetClass, CacheOperationExpressionEvaluator.NO_RESULT);

            AnnotatedElementKey methodCacheKey = new AnnotatedElementKey(method, targetClass);
            // ?????????null????????????
            Object keyValue = evaluator.key(keySpEl, methodCacheKey, evaluationContext);
            return Objects.isNull(keyValue) ? "null" : keyValue;
        }
        return this.keyGenerator.generate(target, method, args);
    }

    /**
     * ???????????????
     *
     * @param target Object
     * @return targetClass
     */
    private Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }


    /**
     * ??????Method
     *
     * @param pjp ProceedingJoinPoint
     * @return {@link Method}
     */
    private Method getSpecificmethod(ProceedingJoinPoint pjp) {
        MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
        Method method = methodSignature.getMethod();
        // The method may be on an interface, but we need attributes from the
        // target class. If the target class is null, the method will be
        // unchanged.
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pjp.getTarget());
        if (targetClass == null && pjp.getTarget() != null) {
            targetClass = pjp.getTarget().getClass();
        }
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        // If we are dealing with method with generic parameters, find the
        // original method.
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        return specificMethod;
    }
}