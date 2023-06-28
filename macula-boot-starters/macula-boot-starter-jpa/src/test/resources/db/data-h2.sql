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

-- 初始化插入一些博客信息数据，方便查询.
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('1', '1', '张三-ZhangSan', 'Spring从入门到精通', '这是 Spring 相关的内容', 'admin', '2019-03-01 00:41:33',
        'admin', '2019-03-01 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('2', '1', '李四-LiSi', 'Spring Data JPA 基础教程', '这是 Spring Data JPA 相关的内容', 'admin',
        '2019-05-01 00:41:33', 'admin', '2019-05-01 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('3', '1', '张三-ZhangSan', 'Spring Data JPA 文档翻译', '这是 Spring Data JPA 文档翻译的内容', 'admin',
        '2019-07-01 00:41:33', 'admin', '2019-07-01 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('4', '1', '张三-ZhangSan', 'MyBatis 中文教程', '这是 MyBatis 中文教程的内容', 'admin', '2019-07-05 00:41:33',
        'admin', '2019-07-05 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('5', '2', '张三-ZhangSan', 'SpringBoot 快速入门', '这是 SpringBoot 快速入门的内容', 'admin',
        '2019-07-08 00:41:33', 'admin', '2019-07-08 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('6', '2', '张三-WangWu', 'Java 初级教程', '这是 Java 初级教程的内容', 'admin', '2019-07-12 00:41:33', 'admin',
        '2019-07-12 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('7', '3', '王五-WangWu', '分库分表之最佳实践', '这是分库分表之最佳实践的内容', 'admin', '2019-08-01 00:41:33',
        'admin', '2019-08-01 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('8', '3', '王五-WangWu', '你不知道的 CSS 使用技巧', '这是你不知道的 CSS 使用技巧的内容', 'admin',
        '2019-08-03 00:41:33', 'admin', '2019-08-03 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('9', '4', '马六-Maliu', 'Vue 项目实战', '这是Vue 项目实战的内容', 'admin', '2019-08-07 00:41:33', 'admin',
        '2019-08-07 00:41:36');
INSERT INTO T_BLOG(id, c_user_id, c_author, c_title, c_content, create_by, create_time, last_update_by,
                   last_update_time)
VALUES ('10', '5', '马六-Maliu', 'JavaScript 精粹', '这是 JavaScript 精粹的内容', 'admin', '2019-08-13 00:41:33',
        'admin', '2019-08-13 00:41:36');
commit;