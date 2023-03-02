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

package dev.macula.boot.starter.mp.test.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.macula.boot.starter.mp.test.entity.User;
import dev.macula.boot.starter.mp.test.mapper.UserMapper;
import dev.macula.boot.starter.mp.test.service.UserService;
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
