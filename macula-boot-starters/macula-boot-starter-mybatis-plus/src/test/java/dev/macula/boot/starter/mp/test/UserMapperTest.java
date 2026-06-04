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

package dev.macula.boot.starter.mp.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.macula.boot.starter.mp.test.entity.User;
import dev.macula.boot.starter.mp.test.mapper.UserMapper;
import dev.macula.boot.starter.mp.test.vo.UserVO;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 * <b>UserMapperTest</b> 测试User实体
 * </p>
 *
 * @author Rain
 * @since 2022-01-18
 */

@SpringBootTest
@Transactional
public class UserMapperTest {

    @Resource
    private UserMapper userMapper;

    @Test
    public void allTests() throws Exception {
        System.out.println(("----- selectAll method test ------"));
        // 删除所有存在的数据，使用条件避免全表删除被拦截
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(User::getId);
        userMapper.delete(qw);

        // 重新插入初始数据
        dev.macula.boot.starter.mp.test.entity.User user = null;
        user = new dev.macula.boot.starter.mp.test.entity.User();
        user.setName("Jone"); user.setAge(18); user.setEmail("test1@baomidou.com"); userMapper.insert(user);
        user = new dev.macula.boot.starter.mp.test.entity.User();
        user.setName("Jack"); user.setAge(20); user.setEmail("test2@baomidou.com"); userMapper.insert(user);
        user = new dev.macula.boot.starter.mp.test.entity.User();
        user.setName("Tom"); user.setAge(28); user.setEmail("test3@baomidou.com"); userMapper.insert(user);
        user = new dev.macula.boot.starter.mp.test.entity.User();
        user.setName("Sandy"); user.setAge(21); user.setEmail("test4@baomidou.com"); userMapper.insert(user);
        user = new dev.macula.boot.starter.mp.test.entity.User();
        user.setName("Billie"); user.setAge(24); user.setEmail("test5@baomidou.com"); userMapper.insert(user);

        // 1. testSelect
        List<User> userList = userMapper.selectList(null);
        Assertions.assertEquals(5, userList.size());
        userList.forEach(System.out::println);

        // 2. testPage
        IPage<User> userPages = userMapper.selectPage(new Page<>(1, 2), null);
        Assertions.assertEquals(5, userPages.getTotal());
        Assertions.assertEquals(3, userPages.getPages());
        userPages.getRecords().forEach(System.out::println);

        // 3. testFindCustom - 查询第一个用户（Jone）
        List<User> allUsers = userMapper.selectList(null);
        Long firstUserId = allUsers.get(0).getId();
        List<UserVO> vo = userMapper.listById(firstUserId);
        Assertions.assertEquals(1, vo.size());
        Assertions.assertEquals("Jone", vo.get(0).getName());

        // 4. testCryptoField
        String email = "rainsoft@xxx.com";
        User userCrypto = new User();
        userCrypto.setAge(20);
        userCrypto.setEmail(email);
        userCrypto.setName("rain.wang");
        int i = userMapper.insert(userCrypto);
        Assertions.assertTrue(i > 0);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getName, "rain.wang");
        userCrypto = userMapper.selectOne(queryWrapper);

        Assertions.assertEquals(email, userCrypto.getEmail());
        System.out.println(
            "1=======" + userCrypto.getId() + " " + userCrypto.getEmail() + " " + userCrypto.getName() + " " + userCrypto.getVersion() + " " + userCrypto.getLastUpdateTime());

        // 5. testKeyAndFill
        User userKey = new User();
        userKey.setAge(20);
        userKey.setEmail("rainsoft@xxx.com");
        userKey.setName("rain.wang");
        int iKey = userMapper.insert(userKey);
        Assertions.assertTrue(iKey > 0);
        Assertions.assertEquals(0, userKey.getVersion());
        System.out.println(
            "1=======" + userKey.getId() + " " + userKey.getName() + " " + userKey.getVersion() + " " + userKey.getLastUpdateTime());

        User user2 = new User();
        user2.setName("rain02");
        user2.setId(userKey.getId());
        int r = userMapper.updateById(user2);
        Assertions.assertTrue(r > 0);
        Assertions.assertEquals(1, user2.getVersion());
        System.out.println(
            "2=======" + user2.getId() + " " + user2.getName() + " " + user2.getVersion() + " " + user2.getLastUpdateTime());

        User user3 = new User();
        user3.setName("rain03");
        user3.setVersion(1);
        user3.setId(userKey.getId());
        int r2 = userMapper.updateById(user3);
        Assertions.assertTrue(r2 > 0);
        Assertions.assertEquals(2, user3.getVersion());
        System.out.println(
            "3=======" + user3.getId() + " " + user3.getName() + " " + user3.getVersion() + " " + user3.getLastUpdateTime());
        User user4 = userMapper.selectById(userKey.getId());
        System.out.println(
            "4======" + user4.getId() + " " + user4.getName() + " " + user4.getVersion() + " " + user4.getLastUpdateTime());
    }
}
