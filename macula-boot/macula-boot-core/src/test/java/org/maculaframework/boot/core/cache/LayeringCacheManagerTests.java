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

package org.maculaframework.boot.core.cache;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.maculaframework.boot.core.cache.cache.Cache;
import org.maculaframework.boot.core.cache.cache.LayeringCache;
import org.maculaframework.boot.core.cache.cache.caffeine.CaffeineCache;
import org.maculaframework.boot.core.cache.cache.redis.RedisCache;
import org.maculaframework.boot.core.cache.cache.redis.RedisCacheKey;
import org.maculaframework.boot.core.cache.manager.CacheManager;
import org.maculaframework.boot.core.cache.setting.FirstCacheSetting;
import org.maculaframework.boot.core.cache.setting.LayeringCacheSetting;
import org.maculaframework.boot.core.cache.setting.SecondaryCacheSetting;
import org.maculaframework.boot.core.cache.stats.CacheStats;
import org.maculaframework.boot.core.cache.support.ExpireMode;
import org.maculaframework.boot.core.cache.support.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LayeringCacheManagerTests.class)
@SpringBootConfiguration
@ComponentScan
public class LayeringCacheManagerTests {
    private Logger logger = LoggerFactory.getLogger(LayeringCacheManagerTests.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private LayeringCacheSetting layeringCacheSetting1;
    private LayeringCacheSetting layeringCacheSetting2;
    private LayeringCacheSetting layeringCacheSetting4;
    private LayeringCacheSetting layeringCacheSetting5;

    @Before
    public void testGetCache() {
        // ?????? CacheManager getCache??????
        FirstCacheSetting firstCacheSetting1 = new FirstCacheSetting(10, 1000, 4, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting1 = new SecondaryCacheSetting(10, 4, TimeUnit.SECONDS, true, true, 1);
        layeringCacheSetting1 = new LayeringCacheSetting(firstCacheSetting1, secondaryCacheSetting1, "");

        // ????????????????????????null,???????????????1
        FirstCacheSetting firstCacheSetting2 = new FirstCacheSetting(10, 1000, 5, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting2 = new SecondaryCacheSetting(3000, 14, TimeUnit.SECONDS, true, true, 1);
        layeringCacheSetting2 = new LayeringCacheSetting(firstCacheSetting2, secondaryCacheSetting2, "");

        // ????????????????????????null,???????????????10
        FirstCacheSetting firstCacheSetting4 = new FirstCacheSetting(10, 1000, 5, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting4 = new SecondaryCacheSetting(100, 70, TimeUnit.SECONDS, true, true, 10);
        layeringCacheSetting4 = new LayeringCacheSetting(firstCacheSetting4, secondaryCacheSetting4, "");


        // ???????????????????????????null
        FirstCacheSetting firstCacheSetting5 = new FirstCacheSetting(10, 1000, 5, TimeUnit.SECONDS, ExpireMode.WRITE);
        SecondaryCacheSetting secondaryCacheSetting5 = new SecondaryCacheSetting(10, 7, TimeUnit.SECONDS, true, false, 1);
        layeringCacheSetting5 = new LayeringCacheSetting(firstCacheSetting5, secondaryCacheSetting5, "");


        String cacheName = "cache:name";
        Cache cache1 = cacheManager.getCache(cacheName, layeringCacheSetting1);
        Cache cache2 = cacheManager.getCache(cacheName, layeringCacheSetting1);
        Assert.assertEquals(cache1, cache2);

        Cache cache3 = cacheManager.getCache(cacheName, layeringCacheSetting2);
        Collection<Cache> caches = cacheManager.getCache(cacheName);
        Assert.assertTrue(caches.size() == 2);
        Assert.assertNotEquals(cache1, cache3);
    }


    @Test
    public void testCacheExpiration() {
        // ?????? ??????????????????
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key1";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        // ????????????????????????????????????
        String str1 = cache1.getFirstCache().get(cacheKey1, String.class);
        String st2 = cache1.getFirstCache().get(cacheKey1, () -> initCache(String.class));
        logger.debug("========================:{}", str1);
        Assert.assertTrue(str1.equals(st2));
        Assert.assertTrue(str1.equals(initCache(String.class)));
        sleep(5);
        Assert.assertNull(cache1.getFirstCache().get(cacheKey1, String.class));
        // ????????????????????????????????????
        cache1.get(cacheKey1, () -> initCache(String.class));

        // ??????????????????
        str1 = cache1.getSecondCache().get(cacheKey1, String.class);
        st2 = cache1.getSecondCache().get(cacheKey1, () -> initCache(String.class));
        Assert.assertTrue(st2.equals(str1));
        Assert.assertTrue(str1.equals(initCache(String.class)));
        sleep(5);
        // ????????????????????????????????????
        RedisCacheKey redisCacheKey = ((RedisCache) cache1.getSecondCache()).getRedisCacheKey(cacheKey1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        sleep(6);
        Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        logger.debug("========================ttl 1:{}", ttl);
        Assert.assertNotNull(cache1.getSecondCache().get(cacheKey1));
        sleep(5);
        ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        logger.debug("========================ttl 2:{}", ttl);
        Assert.assertNull(cache1.getSecondCache().get(cacheKey1));
    }

    @Test
    public void testGetCacheNullUserAllowNullValueTrue() {
        logger.info("???????????????????????????NULL???NULL??????????????????10");
        // ?????? ??????????????????
        String cacheName = "cache:name:118_1";
        String cacheKey1 = "cache:key1:118_1";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting4);
        cache1.get(cacheKey1, () -> initNullCache());
        // ?????????????????????????????????NULL
        CaffeineCache firstCache = (CaffeineCache) cache1.getFirstCache();
        String str1 = firstCache.get(cacheKey1, String.class);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = firstCache.getNativeCache();
        Assert.assertTrue(str1 == null);
        Assert.assertTrue(0 == nativeCache.asMap().size());

        // ???????????????????????????NULL??????NULL??????????????????10
        String st2 = cache1.getSecondCache().get(cacheKey1, String.class);
        RedisCacheKey redisCacheKey = ((RedisCache) cache1.getSecondCache()).getRedisCacheKey(cacheKey1);
        Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Assert.assertTrue(redisTemplate.hasKey(redisCacheKey.getKey()));
        Assert.assertTrue(st2 == null);
        Assert.assertTrue(ttl <= 10);
        sleep(5);
        st2 = cache1.getSecondCache().get(cacheKey1, String.class);
        Assert.assertTrue(st2 == null);
        cache1.getSecondCache().get(cacheKey1, () -> initNullCache());
        sleep(1);
        ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Assert.assertTrue(ttl <= 10 && ttl > 5);

        st2 = cache1.get(cacheKey1, String.class);
        Assert.assertTrue(st2 == null);
    }

    @Test
    public void testGetCacheNullUserAllowNullValueFalse() {
        logger.info("??????????????????????????????NULL");
        // ?????? ??????????????????
        String cacheName = "cache:name:118_2";
        String cacheKey1 = "cache:key1:118_2";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting5);
        cache1.get(cacheKey1, () -> initNullCache());
        // ?????????????????????????????????NULL

        CaffeineCache firstCache = (CaffeineCache) cache1.getFirstCache();
        String str1 = firstCache.get(cacheKey1, String.class);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = firstCache.getNativeCache();
        Assert.assertTrue(str1 == null);
        Assert.assertTrue(0 == nativeCache.asMap().size());

        // ??????????????????????????????NULL??????NULL??????????????????10
        String st2 = cache1.getSecondCache().get(cacheKey1, String.class);
        RedisCacheKey redisCacheKey = ((RedisCache) cache1.getSecondCache()).getRedisCacheKey(cacheKey1);
        Assert.assertTrue(!redisTemplate.hasKey(redisCacheKey.getKey()));
        Assert.assertTrue(st2 == null);
    }

    @Test
    public void testGetType() throws Exception {
        // ?????? ??????????????????
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key:22";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache1.get(cacheKey1, () -> null);
        String str1 = cache1.get(cacheKey1, String.class);
        Assert.assertNull(str1);
        sleep(11);
        cache1.get(cacheKey1, () -> initCache(String.class));

        str1 = cache1.get(cacheKey1, String.class);
        Assert.assertEquals(str1, initCache(String.class));
    }

    @Test
    public void testCacheEvict() throws Exception {
        // ?????? ??????????????????
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key2";
        String cacheKey2 = "cache:key3";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        cache1.get(cacheKey2, () -> initCache(String.class));
        // ??????????????????
        cache1.evict(cacheKey1);
        Thread.sleep(500);
        String str1 = cache1.get(cacheKey1, String.class);
        String str2 = cache1.get(cacheKey2, String.class);
        Assert.assertNull(str1);
        Assert.assertNotNull(str2);
        // ??????????????????
        cache1.evict(cacheKey1);
        Thread.sleep(500);
        str1 = cache1.get(cacheKey1, () -> initCache(String.class));
        str2 = cache1.get(cacheKey2, String.class);
        Assert.assertNotNull(str1);
        Assert.assertNotNull(str2);
    }

    @Test
    public void testCacheClear() throws Exception {
        // ?????? ??????????????????
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key4";
        String cacheKey2 = "cache:key5";
        LayeringCache cache = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache.get(cacheKey1, () -> initCache(String.class));
        cache.get(cacheKey2, () -> initCache(String.class));
        // ??????????????????
        cache.clear();
        Thread.sleep(500);
        String str1 = cache.get(cacheKey1, String.class);
        String str2 = cache.get(cacheKey2, String.class);
        Assert.assertNull(str1);
        Assert.assertNull(str2);
        // ??????????????????
        cache.clear();
        Thread.sleep(500);
        str1 = cache.get(cacheKey1, () -> initCache(String.class));
        str2 = cache.get(cacheKey2, () -> initCache(String.class));
        Assert.assertNotNull(str1);
        Assert.assertNotNull(str2);
    }

    @Test
    public void testCachePut() throws Exception {
        // ?????? ??????????????????
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key6";
        LayeringCache cache = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        String str1 = cache.get(cacheKey1, String.class);
        Assert.assertNull(str1);

        cache.put(cacheKey1, "test1");
        str1 = cache.get(cacheKey1, String.class);
        Assert.assertEquals(str1, "test1");

        cache.put(cacheKey1, "test2");
        Thread.sleep(2000);
        Object value = cache.getFirstCache().get(cacheKey1);
        Assert.assertNull(value);
        str1 = cache.get(cacheKey1, String.class);
        Assert.assertEquals(str1, "test2");
    }

    @Test
    public void testPutCacheNullUserAllowNullValueTrue() {
        logger.info("??????Put?????????????????????NULL???NULL??????????????????10");
        // ?????? ??????????????????
        String cacheName = "cache:name:118_3";
        String cacheKey1 = "cache:key1:118_3";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting4);
        cache1.put(cacheKey1, initNullCache());
        // ?????????????????????????????????NULL
        CaffeineCache firstCache = (CaffeineCache) cache1.getFirstCache();
        String str1 = firstCache.get(cacheKey1, String.class);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = firstCache.getNativeCache();
        Assert.assertTrue(str1 == null);
        Assert.assertTrue(0 == nativeCache.asMap().size());

        // ???????????????????????????NULL??????NULL??????????????????10
        String st2 = cache1.getSecondCache().get(cacheKey1, String.class);
        RedisCacheKey redisCacheKey = ((RedisCache) cache1.getSecondCache()).getRedisCacheKey(cacheKey1);
        Long ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Assert.assertTrue(redisTemplate.hasKey(redisCacheKey.getKey()));
        Assert.assertTrue(st2 == null);
        Assert.assertTrue(ttl <= 10);
        sleep(5);
        st2 = cache1.getSecondCache().get(cacheKey1, String.class);
        Assert.assertTrue(st2 == null);
        cache1.getSecondCache().get(cacheKey1, () -> initNullCache());
        sleep(1);
        ttl = redisTemplate.getExpire(redisCacheKey.getKey());
        Assert.assertTrue(ttl <= 10 && ttl > 5);

        st2 = cache1.get(cacheKey1, String.class);
        Assert.assertTrue(st2 == null);
    }

    @Test
    public void testCacheNullUserAllowNullValueFalse() {
        logger.info("??????Put????????????????????????NULL");
        // ?????? ??????????????????
        String cacheName = "cache:name:118_4";
        String cacheKey1 = "cache:key1:118_4";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting5);
        cache1.put(cacheKey1, initNullCache());
        // ?????????????????????????????????NULL
        CaffeineCache firstCache = (CaffeineCache) cache1.getFirstCache();
        String str1 = firstCache.get(cacheKey1, String.class);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = firstCache.getNativeCache();
        Assert.assertTrue(str1 == null);
        Assert.assertTrue(0 == nativeCache.asMap().size());

        // ??????????????????????????????NULL??????NULL??????????????????10
        String st2 = cache1.getSecondCache().get(cacheKey1, String.class);
        RedisCacheKey redisCacheKey = ((RedisCache) cache1.getSecondCache()).getRedisCacheKey(cacheKey1);
        Assert.assertTrue(!redisTemplate.hasKey(redisCacheKey.getKey()));
        Assert.assertTrue(st2 == null);
    }

    @Test
    public void testCachePutIfAbsent() throws Exception {
        // ?????? ??????????????????
        String cacheName = "cache:name";
        String cacheKey1 = "cache:key7";
        LayeringCache cache = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache.putIfAbsent(cacheKey1, "test1");
        Thread.sleep(2000);
        Object value = cache.getFirstCache().get(cacheKey1);
        Assert.assertNull(value);
        String str1 = cache.get(cacheKey1, String.class);
        Assert.assertEquals(str1, "test1");

        cache.putIfAbsent(cacheKey1, "test2");
        str1 = cache.get(cacheKey1, String.class);
        Assert.assertEquals(str1, "test1");
    }


    /**
     * ????????????
     */
    @Test
    public void testStats() {
        // ?????? ??????????????????
        String cacheName = "cache:name:1";
        String cacheKey1 = "cache:key:123";
        LayeringCache cache1 = (LayeringCache) cacheManager.getCache(cacheName, layeringCacheSetting1);
        cache1.get(cacheKey1, () -> initCache(String.class));
        cache1.get(cacheKey1, () -> initCache(String.class));
        sleep(5);
        cache1.get(cacheKey1, () -> initCache(String.class));

        sleep(11);
        cache1.get(cacheKey1, () -> initCache(String.class));

        CacheStats cacheStats = cache1.getCacheStats();
        CacheStats cacheStats2 = cache1.getCacheStats();
        Assert.assertEquals(cacheStats.getCacheRequestCount().longValue(), cacheStats2.getCacheRequestCount().longValue());
        Assert.assertEquals(cacheStats.getCachedMethodRequestCount().longValue(), cacheStats2.getCachedMethodRequestCount().longValue());
        Assert.assertEquals(cacheStats.getCachedMethodRequestTime().longValue(), cacheStats2.getCachedMethodRequestTime().longValue());

        logger.debug("???????????????{}", cacheStats.getCacheRequestCount());
        logger.debug("???????????????????????????{}", cacheStats.getCachedMethodRequestCount());
        logger.debug("?????????????????????????????????{}", cacheStats.getCachedMethodRequestTime());

        Assert.assertEquals(cacheStats.getCacheRequestCount().longValue(), 4);
        Assert.assertEquals(cacheStats.getCachedMethodRequestCount().longValue(), 2);
        Assert.assertTrue(cacheStats.getCachedMethodRequestTime().longValue() >= 0);
    }

    /**
     * ?????????
     */
    @Test
    public void testLock() {
        Lock lock = new Lock(redisTemplate, "test:123");
        lock.lock();
        lock.unlock();
    }

    @SuppressWarnings("unchecked")
    private <T> T initCache(Class<T> t) {
        logger.debug("????????????");
        return (T) "test";
    }

    private <T> T initNullCache() {
        logger.debug("????????????,??????");
        return null;
    }


    private void sleep(int time) {
        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}

