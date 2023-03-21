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

package dev.macula.boot.starter.mp.test.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import dev.macula.boot.starter.crypto.annotation.CryptoField;
import dev.macula.boot.starter.mp.entity.BaseEntity;
import dev.macula.boot.starter.mp.test.entity.enums.SexEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * <b>User</b> 用户实体
 * </p>
 *
 * @author Rain
 * @since 2022-01-18
 */

@Getter
@Setter
@ToString
@TableName("T_USER")
public class User extends BaseEntity {

    private String name;

    private Integer age;

    @CryptoField
    private String email;

    private SexEnum sex;

    @Version
    private long version;
}
