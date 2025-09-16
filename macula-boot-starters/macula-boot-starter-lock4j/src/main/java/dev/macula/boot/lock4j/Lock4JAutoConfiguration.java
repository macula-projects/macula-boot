package dev.macula.boot.lock4j;

import com.baomidou.lock.executor.LockExecutor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Lock4J 自动配置
 *
 * @author Gordian
 */
@Configuration
@ConditionalOnClass(Redisson.class)
public class Lock4JAutoConfiguration {

    @Bean
    @Order(1000)
    public LockExecutor<?> redissonNonReentrantLockExecutor(RedissonClient redissonClient) {
        return new RedissonNonReentrantLockExecutor(redissonClient);
    }

}