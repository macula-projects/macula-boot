# 基本开发规范

> Macula 项目基本开发规范，包括环境管理、分支管理、版本管理。

## 环境管理

### 环境命名规范

| 环境名称 | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| local    | 本地环境，默认的开发人员本地的环境                           |
| dev      | 开发环境，用于开发阶段的部署                                 |
| test     | 测试环境，开发提测后测试人员进行测试的环境（有时会和dev公共，减少部署） |
| stg      | 准生产环境，开发阶段测试完成后需要发版前的回归测试环境，代码与生产一致 |
| pet      | 压测环境，用于性能测试                                       |
| prd      | 生产环境                                                     |

### Maven Profile 配置

在 parent 模块中定义了不同环境的 profile，默认激活 local 环境。打包时根据需要加上 `-P` 参数指定环境：

```xml
<profiles>
    <profile>
        <id>local</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <profile.active>local</profile.active>
        </properties>
    </profile>
    <profile>
        <id>dev</id>
        <properties>
            <profile.active>dev</profile.active>
        </properties>
    </profile>
    <profile>
        <id>stg</id>
        <properties>
            <profile.active>stg</profile.active>
        </properties>
    </profile>
    <profile>
        <id>pet</id>
        <properties>
            <profile.active>pet</profile.active>
        </properties>
    </profile>
    <profile>
        <id>prd</id>
        <properties>
            <profile.active>prd</profile.active>
        </properties>
    </profile>
</profiles>
```

### 配置文件规范

微服务模块基于 Spring Boot，配置文件建议：

- `bootstrap.yml`: 配置中心连接信息，不同环境使用 `---` 分隔
- `application.yml`: 应用自身配置，本地环境使用本地文件，其他环境建议通过配置中心定义

Nacos 配置中心命名规范：
- dataId: `${spring.application.name}-profile.yml`
- namespace: 按平台或者业务线命名，不要在 namespace 中携带环境标识

## 分支管理

Macula 采用 **GitHubFlow** 分支管理模型：

### GitHubFlow 特点

- 只存在一个 `main` 主分支，日常开发都合并至 `main`，永远保持其为最新且随时可发布的代码
- 需要添加或修改代码时，基于 `main` 创建分支，提交修改
- 创建 Pull Request，进行代码审查
- 部署到测试环境验证
- 验证通过后合并到 `main` 分支

### 分支命名规约

| 前缀               | 含义 |
| ------------------ | ---- |
| `release/test-**`  | 测试分支，验证后可直接发布的版本 |
| `release/beta-**`  | 用于发布 beta 版本分支 |
| `release/**`       | 用于发布正式稳定版分支 |
| `develop`          | 开发主分支，最新的代码分支 |
| `feature/**`       | 功能开发分支 |
| `bugfix/**`        | 未发布 bug 修复分支 |
| `hotfix/**`        | 已发布 bug 修复分支 |

### 提交命名规约

格式：`[操作类型]操作对象名称`，例如 `[ADD]add IdUtils utility class`

常见操作类型：
- `[IMP]` 提升改善正在开发或者已经实现的功能
- `[FIX]` 修正 BUG
- `[REF]` 重构一个功能，对功能重写
- `[ADD]` 添加实现新功能
- `[REM]` 删除不需要的文件

## 版本管理

Macula 采用 **语义化版本控制**，版本格式：`X.Y.Z`（主版本号.次版本号.修订号）

### 版本号递增规则

- **主版本号**：做了不兼容的 API 修改
- **次版本号**：做了向下兼容的功能性新增
- **修订号**：做了向下兼容的问题修正

### 语义化版本规范要点

1. 标准版本号必须采用 `X.Y.Z` 格式，X、Y、Z 为非负整数，禁止数字前方补零
2. 标记版本号发行后，禁止改变该版本软件的内容，任何修改都必须以新版本发行
3. 主版本号为零 `0.y.z` 的软件处于开发初始阶段，一切都可能随时被改变
4. `1.0.0` 的版本号用于界定公共 API 的形成
5. 修订号 Z 只在只做了向下兼容的修正时才递增
6. 次版本号 Y 在有向下兼容的新功能出现时递增，每当次版本号递增时，修订号必须归零
7. 主版本号 X 在有任何不兼容的修改被加入公共 API 时递增，每当主版本号递增时，次版本号和修订号必须归零

## 构建命令

完整构建（正确处理 revision 需要）：
```bash
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

编译并运行单元测试：
```bash
mvn test
```

不跳过测试构建：
```bash
mvn clean install -Pdeploy
```

运行单个测试：
```bash
mvn test -Dtest=测试类名
mvn test -Dtest=测试类名#方法名
```

发布新版本：
```bash
./release.sh [版本号]
```
