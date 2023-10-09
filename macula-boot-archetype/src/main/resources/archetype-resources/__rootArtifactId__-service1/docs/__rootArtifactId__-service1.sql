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

DROP TABLE IF EXISTS `sapplication`;

CREATE TABLE `application`
(
    `id`               int                                                          NOT NULL AUTO_INCREMENT,
    `application_name` varchar(55) COLLATE utf8mb4_bin                              NOT NULL,
    `code`             varchar(55) COLLATE utf8mb4_bin                              NOT NULL COMMENT '应用编码',
    `ak`               varchar(55) COLLATE utf8mb4_bin                                       DEFAULT NULL,
    `sk`               varchar(55) COLLATE utf8mb4_bin                                       DEFAULT NULL,
    `homepage`         varchar(55) COLLATE utf8mb4_bin                                       DEFAULT NULL,
    `manager`          varchar(55) COLLATE utf8mb4_bin                              NOT NULL COMMENT '负责人',
    `maintainer`       varchar(255) COLLATE utf8mb4_bin                                      DEFAULT NULL COMMENT '维护人',
    `mobile`           varchar(20) COLLATE utf8mb4_bin                              NOT NULL COMMENT '联系方式',
    `access_path`      varchar(255) COLLATE utf8mb4_bin                                      DEFAULT NULL COMMENT '可访问路径',
    `use_attrs`        bigint                                                       NOT NULL DEFAULT '0' COMMENT '回传属性',
    `allowed_attrs`    varchar(255) COLLATE utf8mb4_bin                                      DEFAULT NULL COMMENT '允许的回传属性',
    `create_by`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '*SYSADM' COMMENT '创建人',
    `create_time`      datetime                                                     NOT NULL COMMENT '创建时间',
    `last_update_by`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '*SYSADM' COMMENT '更新人',
    `last_update_time` datetime                                                     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
);