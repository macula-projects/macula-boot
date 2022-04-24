package org.macula.boot.starter.mproot.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.macula.boot.starter.mproot.test.entity.User;
import org.macula.boot.starter.mproot.test.entity.UserSeq;
import org.macula.boot.starter.mproot.test.mapper.UserSeqMapper;
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
