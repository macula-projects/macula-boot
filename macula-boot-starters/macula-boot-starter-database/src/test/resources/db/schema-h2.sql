DROP TABLE IF EXISTS T_USER;

CREATE TABLE T_USER
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name             VARCHAR(100) NULL     DEFAULT NULL COMMENT '姓名',
    age              INT          NULL     DEFAULT NULL COMMENT '年龄',
    email            VARCHAR(50)  NULL     DEFAULT NULL COMMENT '邮箱',
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