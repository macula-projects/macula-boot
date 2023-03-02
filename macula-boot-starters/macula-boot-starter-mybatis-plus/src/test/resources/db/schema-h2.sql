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

DROP TABLE IF EXISTS T_USER;

CREATE TABLE T_USER
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name             VARCHAR(100) NULL     DEFAULT NULL COMMENT '姓名',
    age              INT          NULL     DEFAULT NULL COMMENT '年龄',
    email            VARCHAR(100) NULL     DEFAULT NULL COMMENT '邮箱',
    sex              VARCHAR(1)   NOT NULL DEFAULT 'M' COMMENT '性别',
    create_by        VARCHAR(50)  NOT NULL COMMENT '创建人',
    create_time      TIMESTAMP    NOT NULL COMMENT '创建时间',
    last_update_by   VARCHAR(50)  NOT NULL COMMENT '最后更新人',
    last_update_time VARCHAR(50)  NOT NULL COMMENT '最后更新时间',
    deleted          INT          NOT NULL DEFAULT 0 COMMENT '是否已删除',
    version          BIGINT       NOT NULL DEFAULT 0 COMMENT '版本',
    PRIMARY KEY (id)
);

CREATE SEQUENCE SEQ_USER START WITH 1000
    INCREMENT BY 1
    MINVALUE 1;