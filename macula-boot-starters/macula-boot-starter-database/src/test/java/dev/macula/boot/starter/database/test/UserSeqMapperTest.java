/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.database.test;

import dev.macula.boot.starter.database.test.entity.UserSeq;
import dev.macula.boot.starter.database.test.mapper.UserSeqMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * <p>
 * <b>UserSeqMapperTest</b> UserSeqMapper测试类
 * </p>
 *
 * @author Rain
 * @since 2022-01-23
 */

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserSeqMapperTest {

    @Resource
    private UserSeqMapper userSeqMapper;

    @Test
    public void testKeyGen() {
        UserSeq user = new UserSeq();
        user.setAge(20);
        user.setEmail("rainsoft@xxx.com");
        user.setName("rain.wang");
        int i = userSeqMapper.insert(user);
        Assertions.assertTrue(i > 0);
        System.out.println("1=======" + user.getId() + " " + user.getName() + " " + user.getVersion() + " " + user.getLastUpdateTime());

        UserSeq user2 = new UserSeq();
        user2.setAge(20);
        user2.setEmail("rainsoft@xxx.com");
        user2.setName("rain.wang");
        int r = userSeqMapper.insert(user2);
        Assertions.assertTrue(r > 0);
        System.out.println("2=======" + user2.getId() + " " + user2.getName() + " " + user2.getVersion() + " " + user2.getLastUpdateTime());
    }

    @Test
    public void testLogicDelete() {
        int r = userSeqMapper.deleteById(new UserSeq(1L));
        Assertions.assertTrue( r > 0);
    }
}
