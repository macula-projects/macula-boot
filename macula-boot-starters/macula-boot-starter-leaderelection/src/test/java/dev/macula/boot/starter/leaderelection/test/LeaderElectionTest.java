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

import dev.macula.boot.starter.leaderelection.ElectionListener;
import dev.macula.boot.starter.leaderelection.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code LeaderElectionTest} 领导选举测试
 *
 * @author rain
 * @since 2022/12/3 17:04
 */
public class LeaderElectionTest {

    private RedissonClient redissonClient;

    @BeforeEach
    public void setUp() throws Exception {
        Config config = Config.fromYAML(getClass().getResource("/redisson.yml"));
        redissonClient = Redisson.create(config);
    }

    @Test
    public void testElection() throws Exception {
        Map<String, Boolean> electionState = new ConcurrentHashMap<>();
        Map<String, LeaderElection> elections = new ConcurrentHashMap<>();
        for (int i = 0; i < 5; i++) {
            final Thread thread = new Thread("service" + i) {
                @Override
                public void run() {
                    LeaderElection leaderElection = new LeaderElection();
                    leaderElection.setRedissonClient(redissonClient);
                    leaderElection.addElectionListener(new ElectionListener() {
                        @Override
                        public void onElected() {
                            System.out.println(getName() + "=============:=============selected");
                        }
                    });
                    leaderElection.tryHold("leader-lock");
                    elections.put(getName(), leaderElection);
                    while (true) {
                        if (leaderElection.isMaster()) {
                            System.out.println(getName() + "===============:============" + leaderElection.isMaster());
                        }
                        electionState.put(getName(), leaderElection.isMaster());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {

                        }
                    }
                }
            };
            thread.start();
        }

        waitAndQuit(electionState, elections);

        waitAndQuit(electionState, elections);

        waitAndQuit(electionState, elections);

        waitAndQuit(electionState, elections);

        waitAndQuit(electionState, elections);

        waitAndQuit(electionState, elections);

        Thread.sleep(3000);

        for (LeaderElection leaderElection : elections.values()) {
            leaderElection.shutdown();
        }
    }

    private void waitAndQuit(Map<String, Boolean> electionState, Map<String, LeaderElection> elections)
        throws Exception {
        Thread.sleep(3000);

        for (Map.Entry<String, Boolean> entry : electionState.entrySet()) {
            if (entry.getValue()) {
                elections.get(entry.getKey()).shutdown();
                System.out.println(entry.getKey() + ": shutdown, ===============no master================");
                return;
            }
        }
    }
}
