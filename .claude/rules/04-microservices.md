# 微服务开发规范

> 微服务项目工程结构与命名规范。

## 微服务工程模块命名

一个微服务平台由多个模块组成，遵循以下命名规范：

| 模块名称                      | 说明                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| `xxx-parent`                  | 父工程，定义 Maven 依赖统一版本管理                           |
| `xxx-gateway`                 | 微服务网关                                                   |
| `xxx-admin-bff`               | 管理控制台的 API，聚合微服务层的接口，不会作为微服务给其他模块访问 |
| `xxx-openapi`                 | 给第三方平台访问的开放 API                                    |
| `xxx-xxx-bff`                 | 其他前端的 API 层，不同前端不同的 BFF 模块                   |
| `xxx-basic`                   | 微服务模块，用于基础的服务，可选项                           |
| `xxx-thirdparty`              | 微服务模块，用于集中访问第三方系统，防腐                     |
| `xxx-[service-name]`          | 具体各个业务的微服务模块，按照领域命名                       |
| `xxx-[service-name]-api`      | 微服务模块的对外 SDK，定义了其他模块访问该模块的接口以及 POJO 类 |

## 项目依赖关系

- **API 模块**：只存放接口定义和 DTO 类，不包含实现
- **业务模块**：实现 API 接口，依赖 API 模块
- **消费者模块**：依赖 API 模块，调用远程服务

## 依赖引入规范

创建新的业务微服务模块，建议引入以下依赖：

```xml
<!-- Web 应用基础依赖包，统一处理返回对象及异常等 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-web</artifactId>
</dependency>

<!-- 应用安全基础依赖包，主要获取用户上下文及接口安全拦截等 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-security</artifactId>
</dependency>

<!-- 基于 SpringDoc 的 OpenAPI 文档 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-springdoc</artifactId>
</dependency>

<!-- MySQL + Druid + MyBatis-Plus 基础依赖包 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-mybatis-plus</artifactId>
</dependency>

<!-- 基于 mapstruct 对象转换 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-mapstruct</artifactId>
</dependency>

<!-- 基于 Redisson Redis 客户端 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-redis</artifactId>
</dependency>

<!-- 阿里云微服务治理 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-alibaba</artifactId>
</dependency>

<!-- 腾讯云微服务治理 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-tencent</artifactId>
</dependency>
```

## 多云厂商支持

Macula 支持同时接入阿里云和腾讯云：

- **Spring Cloud Alibaba**：Nacos 注册配置中心 + Sentinel 限流
- **Spring Cloud Tencent**：Polaris 注册配置中心

根据实际部署环境选择合适的 starter 即可。

## 数据库设计规范

### 基础字段

所有表建议包含以下公共字段：

```sql
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
`create_by` varchar(50) NOT NULL DEFAULT '*SYSADM' COMMENT '创建人',
`create_time` datetime NOT NULL COMMENT '创建时间',
`last_update_by` varchar(50) NOT NULL DEFAULT '*SYSADM' COMMENT '更新人',
`last_update_time` datetime NOT NULL COMMENT '更新时间',
PRIMARY KEY (`id`)
```

MyBatis-Plus 自动填充配置：
- `createBy` / `createTime`: `FieldFill.INSERT`
- `lastUpdateBy` / `lastUpdateTime`: `FieldFill.INSERT_UPDATE`

### 逻辑删除

建议使用逻辑删除：

```sql
`deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
```

MyBatis-Plus 注解：
```java
@TableLogic(value = "0", delval = "1")
private Integer deleted;
```
