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

package dev.macula.boot.starter.leaderelection.test;

import dev.macula.boot.starter.leaderelection.LeaderElection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

/**
 * {@code Application} 启动类
 *
 * @author rain
 * @since 2022/12/3 19:12
 */
@SpringBootApplication
public class LeaderElectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaderElectionApplication.class, args);
    }

    @Component
    public class Service {
        public Service(LeaderElection leaderElection) {
            leaderElection.addElectionListener(() -> System.out.println("master selected"));
        }
    }
}
