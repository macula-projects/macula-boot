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

package dev.macula.boot.starter.leaderelection.config;

import dev.macula.boot.starter.leaderelection.LeaderElection;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@code LeaderAutoConfiguration} 领导选举自动配置
 *
 * @author rain
 * @since 2022/12/3 16:41
 */
@AutoConfiguration
public class LeaderAutoConfiguration {

    @Value("${spring.application.name}")
    private String appName;

    @Bean(destroyMethod = "shutdown")
    public LeaderElection leaderElection(RedissonClient redissonClient) {
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.setRedissonClient(redissonClient);
        leaderElection.tryHold("leader-lock-" + appName);
        return leaderElection;
    }
}
