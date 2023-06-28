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

package dev.macula.boot.starter.jpa.test.entity;

import dev.macula.boot.starter.jpa.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 博客信息实体类.
 *
 * @author blinkfox on 2019-08-16.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "t_blog")
public class Blog extends BaseEntity<String> {

    /**
     * 发表博客的用户 ID.
     */
    @Column(name = "c_user_id")
    private String userId;

    /**
     * 博客标题.
     */
    @Column(name = "c_title")
    private String title;

    /**
     * 博客作者.
     */
    @Column(name = "c_author")
    private String author;

    /**
     * 博客内容.
     */
    @Column(name = "c_content")
    private String content;

}
