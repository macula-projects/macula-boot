package org.macula.boot.starter.database.test;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.*;
import org.macula.boot.starter.database.test.entity.User;
import org.macula.boot.starter.database.test.mapper.UserMapper;
import org.macula.boot.starter.database.test.vo.UserVO;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserMapperTest {
    @Resource
    private UserMapper userMapper;

    @Test
    @Order(1)
    public void testSelect() {
        System.out.println(("----- selectAll method test ------"));
        List<User> userList = userMapper.selectList(null);
        Assertions.assertEquals(5, userList.size());
        userList.forEach(System.out::println);
    }

    @Test
    @Order(2)
    public void testPage() {
        IPage<User> userPages = userMapper.selectPage(new Page<>(1, 2), null);
        Assertions.assertEquals(5, userPages.getTotal());
        Assertions.assertEquals(3, userPages.getPages());
        userPages.getRecords().forEach(System.out::println);
    }

    @Test
    @Order(100)
    public void testKeyAndFill() throws Exception {
        User user = new User();
        user.setAge(20);
        user.setEmail("rainsoft@xxx.com");
        user.setName("rain.wang");
        int i = userMapper.insert(user);
        Assertions.assertTrue(i > 0);
        Assertions.assertEquals(0, user.getVersion());
        System.out.println("1=======" + user.getId() + " " + user.getName() + " " + user.getVersion() + " " + user.getLastUpdateTime());

        User user2 = new User();
        user2.setName("rain02");
        user2.setId(user.getId());
        int r = userMapper.updateById(user2);
        Assertions.assertTrue(r > 0);
        Assertions.assertEquals(1, user2.getVersion());
        System.out.println("2=======" + user2.getId() + " " + user2.getName() + " " + user2.getVersion() + " " + user2.getLastUpdateTime());

        User user3 = new User();
        user3.setName("rain03");
        user3.setVersion(1);
        user3.setId(user.getId());
        int r2 = userMapper.updateById(user3);
        Assertions.assertTrue(r2 > 0);
        Assertions.assertEquals(2, user3.getVersion());
        System.out.println("3=======" + user3.getId() + " " + user3.getName() + " " + user3.getVersion() + " " + user3.getLastUpdateTime());
        User user4 = userMapper.selectById(6L);
        System.out.println("4======" + user4.getId() + " " + user4.getName() + " " + user4.getVersion() + " " + user4.getLastUpdateTime());
    }

    @Test
    public void testFindCustom() {
        List<UserVO> vo = userMapper.listById(1L);
        Assertions.assertEquals(1, vo.size());
        Assertions.assertEquals("Jone", vo.get(0).getName());
    }
}
