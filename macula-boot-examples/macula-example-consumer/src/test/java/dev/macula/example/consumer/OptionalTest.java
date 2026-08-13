/*
 * Copyright (c) 2023-2026 Macula
 * macula.dev, China
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

package dev.macula.example.consumer;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * <p>
 * <b>OptionalTest</b> Optional使用测试
 * </p>
 *
 * @author Rain
 * @since 2024/3/22
 */
public class OptionalTest {
    Person person = new Person();

    @Test
    public void testNull() {
        System.out.println(
                Optional.ofNullable(person)
                        .map(Person::getUser)
                        .map(User::getAddress)
                        .map(Address::getStreet).orElse("default")
        );
    }
}

/**
 * Optional 链式访问测试中的人员模型。
 *
 * @author rain
 * @since 5.0.0
 */
@Getter
class Person {
    private User user = new User();
}

/**
 * Optional 链式访问测试中的用户模型。
 *
 * @author rain
 * @since 5.0.0
 */
@Getter
class User {
    private Address address = new Address();
}

/**
 * Optional 链式访问测试中的地址模型。
 *
 * @author rain
 * @since 5.0.0
 */
@Getter
class Address {
    private String street = "abc";
}

