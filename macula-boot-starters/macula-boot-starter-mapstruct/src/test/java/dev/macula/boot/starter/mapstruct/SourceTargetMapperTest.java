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

package dev.macula.boot.starter.mapstruct;

import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

public class SourceTargetMapperTest {
    private static final Converter converter = new Converter();

    @Test
    public void testMapper() {
        SourceDto s = new SourceDto("rain", "123456", new Date(), LocalDateTime.now(), ZonedDateTime.now().now(), 90);
        TargetDto t = SourceTargetMapper.MAPPER.toTarget(s);
        System.out.println(t);
    }

    @Test
    public void testAutoMapper() {
        User user = new User();
        user.setUsername("jack");
        user.setAge(23);
        user.setYoung(false);

        UserDto userDto = converter.convert(user, UserDto.class);

        System.out.println(userDto);    // UserDto{username='jack', age=23, young=false}

        assert user.getUsername().equals(userDto.getUsername());
        assert user.getAge() == userDto.getAge();
        assert user.isYoung() == userDto.isYoung();

        User newUser = converter.convert(userDto, User.class);

        System.out.println(newUser);    // User{username='jack', age=23, young=false}

        assert user.getUsername().equals(newUser.getUsername());
        assert user.getAge() == newUser.getAge();
        assert user.isYoung() == newUser.isYoung();
    }
}
