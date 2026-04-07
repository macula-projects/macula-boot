# AGENTS.md

本文档为AI Coding智能体在本代码仓库工作时提供指导。

## 构建命令

**使用 deploy 配置文件完整构建（正确处理 revision 需要）：**
```bash
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

**编译并运行单元测试：**
```bash
mvn test
```

**不跳过测试构建：**
```bash
mvn clean install -Pdeploy
```

**运行单个测试：**
```bash
mvn test -Dtest=测试类名
mvn test -Dtest=测试类名#方法名
```

**发布新版本：**
```bash
./release.sh [版本号]
```

## 代码架构

Macula Boot 是一个基于 Spring Boot 和 Spring Cloud（阿里巴巴/腾讯云）的 Java 微服务开发框架。它提供了一组预先构建的启动器和工具模块，加速微服务开发。

### 项目结构

```
macula-boot/
├── macula-boot-parent/          # 父 POM，提供依赖版本管理
├── macula-boot-commons/          # 通用工具类、异常、结果包装器
├── macula-boot-starters/         # 所有启动器模块的集合
│   ├── macula-boot-starter-web/           # Web 支持
│   ├── macula-boot-starter-mybatis-plus/  # MyBatis Plus 集成
│   ├── macula-boot-starter-redis/         # Redis/Redisson 集成
│   ├── macula-boot-starter-security/      # 安全认证
│   ├── macula-boot-starter-rocketmq/      # RocketMQ 集成
│   ├── macula-boot-starter-cloud-alibaba/ # Spring Cloud Alibaba（Nacos、Sentinel）
│   ├── macula-boot-starter-cloud-tencent/ # Spring Cloud Tencent（Polaris）
│   └── ... 其他启动器 ...
├── macula-boot-examples/         # 演示用法的示例应用
└── macula-boot-archetype/        # Maven 项目生成骨架
```

### 核心模块

1. **macula-boot-parent**：为所有模块定义一致的依赖版本和插件配置。所有 Macula 项目都继承自此父 POM。

2. **macula-boot-commons**：提供基础异常类（`MaculaException`、`BizException`）、结果包装器（`Result`、`PageVO`）、租户/灰度版本上下文持有器，以及通用工具类。

3. **macula-boot-starters**：每个启动器为特定功能提供自动配置：
   - **持久化**：mybatis-plus、jpa、redis、cache
   - **消息**：rocketmq、kafka、sender（持久化消息）
   - **云原生**：alibaba-nacos、tencent-polaris、gateway、tsf
   - **安全**：security、oauth2、crypto
   - **工具**：mapstruct、lock4j、幂等、oss、tinyid、task、retry

4. **macula-boot-examples**：可运行示例，包括网关、服务提供者/消费者、DDD、任务调度和云厂商集成。

### 技术栈

- **Java**：17（6.0.0-SNAPSHOT）
- **Spring Boot**：3.5.8
- **Spring Cloud**：2023.0.6
- **Spring Cloud Alibaba**：2023.0.3.4
- **Spring Cloud Tencent**：2.1.0.0-2023.0.6

### 开发注意事项

- 完整构建项目时**必须**使用 `-Pdeploy` Maven 配置文件才能正确处理 `${revision}` 属性
- 项目支持多个云厂商：阿里云、腾讯云和混合部署
- 所有启动器都遵循 Spring Boot 自动配置约定
- 代码按功能/能力组织在 starters 目录结构中
