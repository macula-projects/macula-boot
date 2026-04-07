# 开发工作流程

> Macula 项目开发完整工作流程。

## 开发环境准备

### 后端环境要求

- Git
- JDK 17 （Macula 6.x 使用 Java 17）
- Maven 3.6+
- IDE (IDEA 推荐)
- MySQL 8.0+
- Redis
- Nacos (微服务注册配置中心)

## 创建服务模块开发流程

### 1. 创建 Maven 模块

根据业务领域创建微服务模块：
- 创建 `xxx-service`：业务实现模块
- 创建 `xxx-service-api`：对外接口定义模块

### 2. 引入依赖

在业务模块 pom.xml 中引入所需 Macula starter 依赖，详见 [微服务规范](04-microservices.md)

### 3. 数据库表设计

- 每个表必须包含基础字段：`id`、`create_by`、`create_time`、`last_update_by`、`last_update_time`
- 建议使用逻辑删除字段 `deleted`
- 表名使用下划线分隔，避免使用保留字
- 必须添加必要的索引

### 4. 代码分层实现

按照应用分层规范：

1. **数据存取层**
   - 创建 Entity 实体类
   - 创建 Mapper 接口
   - 自定义 SQL 创建 XML 文件

2. **业务逻辑层**
   - 创建 Query、Form、BO 等对象
   - 创建 Service 接口
   - 创建 Service 实现类

3. **展示层**
   - 创建 VO 对象
   - 创建 Controller
   - 添加 OpenAPI 注解

### 5. 编码检查清单

- [ ] Controller 没有业务逻辑，只做参数接收和结果返回
- [ ] Service 方法添加事务注解 `@Transactional`
- [ ] 参数校验已完成
- [ ] 异常使用 `BizException` 抛出
- [ ] 不手动返回 Result ，框架自动包装
- [ ] OpenAPI 注释已添加
- [ ] SQL 没有字符串拼接

### 6. 本地运行

- 本地使用 `local` profile
- 配置文件 `bootstrap.yml` 中配置 Nacos 连接信息
- 启动 Spring Boot 应用

### 7. 代码提交

- 按照提交命名规范：`[ADD]add feature name`
- 创建 Pull Request
- 代码审查通过后合并到 main 分支

## 项目构建

完整构建命令：
```bash
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

## 技术栈版本

当前 Macula 版本：
- Java: 17
- Spring Boot: 3.5.8
- Spring Cloud: 2023.0.6
- Spring Cloud Alibaba: 2023.0.3.4
- Spring Cloud Tencent: 2.1.0.0-2023.0.6

## Claude Code 编码规则

在本项目使用 Claude Code 编码时，请遵循以下规则：

1. **遵循现有代码风格**：查看已有代码的包结构、命名方式，保持一致
2. **不要删除原有注释**：除非用户要求，否则不要修改原有作者注释
3. **不添加不必要的注释**：只在逻辑不清晰的地方添加注释
4. **使用项目已有的抽象**：不要创建新的工具类当已有相同功能
5. **遵守强制规范**：特别是 API 设计、对象分层、中间件命名必须遵循本目录下的规范
6. **小步前进**：大功能分多次提交，不要一次改动太多代码
