# Intent: 校正 MyBatis-Plus 基类审计字段说明
Author: 用户. Status: accepted.

## Problem

`BaseEntity` 的 `createTime` 被标注成“更新人”，`lastUpdateBy` 被标注成“创建时间”。从实体生成的接口模型说明与两项字段的实际含义相反。

## Proposed outcome

`createTime` 显示“创建时间”，`lastUpdateBy` 显示“更新人”。基类四个审计字段的说明与用户截图及 Cloud IAM 审计字段含义一致。

## Affected users and systems

Macula Boot MyBatis-Plus Starter 及使用 `BaseEntity` 生成接口文档的维护者。Macula Cloud 的管理配置审计实施在 Cloud 仓库独立跟踪。

## Constraints

本项限于用户标注的 `BaseEntity.java` 两项接口说明。字段名、类型、持久化映射及自动填充行为沿用现有约定。

## Open questions

无。Cloud 的操作人标识、字段长度和历史数据处理由配套需求确认。
