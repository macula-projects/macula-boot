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

package dev.macula.boot.starter.redis.config;

import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * <b>RedissonWrapper</b> Redisson创建包裹类
 * </p>
 *
 * @author Rain
 * @since 2022-01-28
 */
public class RedissonConfigBuilder {

    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

    public static RedissonConfigBuilder create() {
        return new RedissonConfigBuilder();
    }

    public Config build(ApplicationContext ctx, RedissonProperties redissonProperties) throws IOException {
        return build(ctx, null, redissonProperties);
    }

    public Config build(ApplicationContext ctx, DataRedisProperties redisProperties,
        RedissonProperties redissonProperties)
        throws IOException {
        if (redisProperties == null) {
            redisProperties = new DataRedisProperties();
        }

        if (StringUtils.hasText(redisProperties.getSsl().getBundle())) {
            throw new IllegalArgumentException("Redisson does not support Spring Boot SSL bundles; "
                + "configure Redisson TLS through spring.redis.redisson YAML instead");
        }
        String protocolPrefix = redisProperties.getSsl().isEnabled()
            ? REDISS_PROTOCOL_PREFIX : REDIS_PROTOCOL_PREFIX;

        Config config = null;
        Method clusterMethod = ReflectionUtils.findMethod(DataRedisProperties.class, "getCluster");
        Method timeoutMethod = ReflectionUtils.findMethod(DataRedisProperties.class, "getTimeout");
        Object timeoutValue = ReflectionUtils.invokeMethod(timeoutMethod, redisProperties);
        int timeout;
        if (null == timeoutValue) {
            timeout = 10000;
        } else if (!(timeoutValue instanceof Integer)) {
            Method millisMethod = ReflectionUtils.findMethod(timeoutValue.getClass(), "toMillis");
            timeout = ((Long)ReflectionUtils.invokeMethod(millisMethod, timeoutValue)).intValue();
        } else {
            timeout = (Integer)timeoutValue;
        }

        if (redissonProperties.getConfig() != null) {
            try {
                config = Config.fromYAML(redissonProperties.getConfig());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Can't parse Redisson YAML config", e);
            }
        } else if (redissonProperties.getFile() != null) {
            try (InputStream is = getConfigStream(ctx, redissonProperties)) {
                config = Config.fromYAML(is);
            } catch (IOException | RuntimeException e) {
                throw new IllegalArgumentException("Can't parse Redisson YAML config", e);
            }
        } else if (redisProperties.getSentinel() != null) {
            Method nodesMethod = ReflectionUtils.findMethod(DataRedisProperties.Sentinel.class, "getNodes");
            Object nodesValue = ReflectionUtils.invokeMethod(nodesMethod, redisProperties.getSentinel());

            String[] nodes;
            if (nodesValue instanceof String) {
                nodes = convert(Arrays.asList(((String)nodesValue).split(",")), protocolPrefix);
            } else {
                nodes = convert((List<String>)nodesValue, protocolPrefix);
            }

            config = new Config().setUsername(redisProperties.getUsername())
                .setPassword(redisProperties.getPassword());
            config.useSentinelServers().setMasterName(redisProperties.getSentinel().getMaster())
                .addSentinelAddress(nodes).setDatabase(redisProperties.getDatabase()).setConnectTimeout(timeout);
        } else if (clusterMethod != null && ReflectionUtils.invokeMethod(clusterMethod, redisProperties) != null) {
            Object clusterObject = ReflectionUtils.invokeMethod(clusterMethod, redisProperties);
            Method nodesMethod = ReflectionUtils.findMethod(clusterObject.getClass(), "getNodes");
            List<String> nodesObject = (List)ReflectionUtils.invokeMethod(nodesMethod, clusterObject);

            String[] nodes = convert(nodesObject, protocolPrefix);

            config = new Config().setUsername(redisProperties.getUsername())
                .setPassword(redisProperties.getPassword());
            config.useClusterServers().addNodeAddress(nodes).setConnectTimeout(timeout);
        } else {
            config = new Config().setUsername(redisProperties.getUsername())
                .setPassword(redisProperties.getPassword());
            config.useSingleServer()
                .setAddress(protocolPrefix + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setConnectTimeout(timeout).setDatabase(redisProperties.getDatabase());
        }

        return config;
    }

    private String[] convert(List<String> nodesObject, String protocolPrefix) {
        List<String> nodes = new ArrayList<String>(nodesObject.size());
        for (String node : nodesObject) {
            if (REDISS_PROTOCOL_PREFIX.equals(protocolPrefix) && node.startsWith(REDIS_PROTOCOL_PREFIX)) {
                nodes.add(REDISS_PROTOCOL_PREFIX + node.substring(REDIS_PROTOCOL_PREFIX.length()));
            } else if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
                nodes.add(protocolPrefix + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[nodes.size()]);
    }

    private InputStream getConfigStream(ApplicationContext ctx, RedissonProperties redissonProperties)
        throws IOException {
        Resource resource = ctx.getResource(redissonProperties.getFile());
        InputStream is = resource.getInputStream();
        return is;
    }
}
