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


-- 创建数据库表.
DROP TABLE IF EXISTS T_BLOG;
CREATE TABLE T_BLOG
(
    id               varchar(32) NOT NULL,
    c_user_id        varchar(255),
    c_author         varchar(255),
    c_title          varchar(255),
    c_content        varchar(255),
    create_by        VARCHAR(50) NOT NULL COMMENT '创建人',
    create_time      TIMESTAMP   NOT NULL COMMENT '创建时间',
    last_update_by   VARCHAR(50) NOT NULL COMMENT '最后更新人',
    last_update_time VARCHAR(50) NOT NULL COMMENT '最后更新时间',
    PRIMARY KEY (id)
);
