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

package dev.macula.boot.starter.database.test.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import dev.macula.boot.starter.database.entity.BaseEntity;

/**
 * <p>
 * <b>UserSeq</b> 使用序列作为ID
 * </p>
 *
 * @author Rain
 * @since 2022-01-23
 */
@Getter
@Setter
@TableName("T_USER")
@KeySequence(value = "SEQ_USER")
public class UserSeq extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String name;

    private Integer age;

    private String email;

    @TableLogic
    private Integer deleted;

    @Version
    private long version;

    public UserSeq() {
    }

    public UserSeq(Long id) {
        this.id = id;
    }
}
