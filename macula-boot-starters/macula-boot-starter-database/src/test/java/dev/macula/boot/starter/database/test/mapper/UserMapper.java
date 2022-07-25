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

package dev.macula.boot.starter.database.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.macula.boot.starter.database.test.entity.User;
import dev.macula.boot.starter.database.test.vo.UserVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * <b>UserMapper</b> User实体的Mapper
 * </p>
 *
 * @author Rain
 * @since 2022-01-18
 */
public interface UserMapper extends BaseMapper<User> {

    List<UserVO> listById(@Param("id") Long id);

}
