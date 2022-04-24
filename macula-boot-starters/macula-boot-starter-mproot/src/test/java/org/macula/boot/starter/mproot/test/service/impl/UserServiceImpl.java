package org.macula.boot.starter.mproot.test.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.macula.boot.starter.mproot.test.entity.User;
import org.macula.boot.starter.mproot.test.mapper.UserMapper;
import org.macula.boot.starter.mproot.test.service.UserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * <b>UserServiceImpl</b> 用户服务接口实现
 * </p>
 *
 * @author Rain
 * @since 2022-01-23
 */

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
