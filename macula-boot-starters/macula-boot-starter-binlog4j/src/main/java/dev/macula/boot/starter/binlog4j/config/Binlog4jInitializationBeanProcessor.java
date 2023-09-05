package dev.macula.boot.starter.binlog4j.config;

import dev.macula.boot.starter.binlog4j.BinlogClient;
import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import dev.macula.boot.starter.binlog4j.IBinlogEventHandler;
import dev.macula.boot.starter.binlog4j.config.annotation.BinlogSubscriber;
import org.redisson.api.RedissonClient;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;

public class Binlog4jInitializationBeanProcessor implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final Map<String, BinlogClientConfig> clientConfigs;

    private final RedissonClient redissonClient;

    public Binlog4jInitializationBeanProcessor(Map<String, BinlogClientConfig> clientConfigs,
        RedissonClient redissonClient) {
        this.clientConfigs = clientConfigs;
        this.redissonClient = redissonClient;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void afterSingletonsInstantiated() {
        Map<String, IBinlogEventHandler> handlers = applicationContext.getBeansOfType(IBinlogEventHandler.class);
        clientConfigs.forEach((clientName, clientConfig) -> {
            BinlogClient client = new BinlogClient(clientConfig, redissonClient);
            handlers.forEach((beanName, handler) -> {
                BinlogSubscriber annotation =
                    AnnotationUtils.findAnnotation(AopUtils.getTargetClass(handler), BinlogSubscriber.class);
                assert annotation != null;
                if (clientName.equals(annotation.clientName())) {
                    client.registerEventHandler(annotation.database(), annotation.table(), handler);
                }
            });
            client.connect();
        });
    }
}
