# Macula Boot Starter MapStruct

## 概述

提供 MapStruct / MapStruct Plus 对象映射依赖，以及 `Date` / `Instant` 与 `LocalDateTime` 的公共双向转换器。映射实现由注解处理器在编译期生成，日期时间转换通过 Starter 自动注册的 Spring 转换器执行。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-mapstruct</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 核心功能

- MapStruct：手写 `@Mapper` 接口，编译时生成实现。
- MapStruct Plus：使用 `@AutoMapper` 自动生成双向 Mapper，通过 Spring 注入的 `io.github.linpeilie.Converter` 调用。
- `DateTimeConverter`：将接口 `Date` / `Instant` 时间点与实体 `LocalDateTime` 按同一存储时区双向转换，支持空值。
- `MapStructAutoConfiguration`：默认注册可覆盖、可禁用的日期时间转换器，无需组件扫描或手工 `@Import`。
- `GlobalMapperConfig`：同时提供普通 MapStruct 和 MapStruct Plus 的编译配置；Parent 默认让 Plus 使用它。

## 使用说明

两种接入方式的区别：

| 使用方式 | 业务代码 | 如何接入公共转换器 |
| --- | --- | --- |
| MapStruct Plus | 模型使用 `@AutoMapper`，注入 `Converter` | 继承 Parent 并引入 Starter 后默认接入，无需显式引用 `GlobalMapperConfig` |
| 普通 MapStruct | 手写 `@Mapper` 接口，注入该接口 | 每个 Mapper 显式设置 `config = GlobalMapperConfig.class` |

两者运行时都由 `MapStructAutoConfiguration` 默认注册 `DateTimeConverter`，不需要业务代码额外添加 `@Import` 或扫描 Starter 包。

### 日期时间与时区约定

实体日期时间保留 `LocalDateTime`，接口需要表达确定时间点时可使用 `Date` 或 `Instant`，已有 `Date` 接口无需批量替换。转换器仅负责对象映射，不负责 JSON 格式或数据库连接时区。

`macula.time.storage-zone` 为可选配置：

| 配置情况 | 转换行为 |
| --- | --- |
| 配置合法时区 | 使用指定时区，忽略首尾空白 |
| 未配置或留空 | 使用创建转换器时的 JVM 默认时区 |
| 配置非法时区 | 初始化失败，避免静默转换为错误时间 |
| 输入 null | 双向均返回 null |

例如：

```yaml
macula:
  time:
    storage-zone: Asia/Shanghai
```

此时 `Date` 表示的 `2026-11-05T00:50:30Z` 映射为实体中的 `2026-11-05T08:50:30`，反向转换还原同一时间点。未配置时使用 JVM 默认时区，不强制使用 UTC。

`LocalDateTime` 本身不携带时区，所有实例应遵守相同的存储时间约定。该配置不改变 JVM、Jackson、JDBC 或数据库时区，也不改写历史数据。

`Instant` 与 `LocalDateTime` 的双向映射遵循同一 `macula.time.storage-zone` 配置，并保留纳秒；`Date` 仅保留毫秒；实际落库精度由数据库列定义决定。接口字段由Date改为Instant时无需另外声明 `uses`，公共配置会按源/目标类型选择对应方法。转换方法使用不同名称，保留原有 `toLocalDateTime(null)` 调用的兼容性。

JSON层建议保留标准ISO格式；`spring.jackson.date-format` 不统一控制Instant格式，不要用固定SSS的日期解析器处理可变精度ISO输入。Java对象转换器不负责JSON格式。

### 情况一：使用 MapStruct Plus

继承更新后的 `macula-boot-parent` 并引入此 Starter 后，直接使用原有 `@AutoMapper`，无需业务模块再写全局配置、`uses` 或 `@Import`：

```java
import io.github.linpeilie.annotations.AutoMapper;
import java.util.Date;
import java.time.Instant;
import java.time.LocalDateTime;

@AutoMapper(target = User.class)
public class UserForm {
    private Date createTime;
    private Instant occurredAt;
    // getter/setter
}

public class User {
    private LocalDateTime createTime;
    private LocalDateTime occurredAt;
    // getter/setter
}
```

注入 Starter 提供的 `io.github.linpeilie.Converter` 后调用：

```java
// 在 Spring 业务 Bean 中注入，不手工 new Converter。
@Autowired
private Converter converter;

// Form 的 Date → Entity 的 LocalDateTime。
User user = converter.convert(form, User.class);
// Entity 的 LocalDateTime → Form 的 Date。
UserForm result = converter.convert(user, UserForm.class);
```

编译期：Parent 传入 `-Amapstruct.plus.mapperConfigClass=dev.macula.boot.mapstruct.GlobalMapperConfig`。Plus 读取其中的 `io.github.linpeilie.annotations.MapperConfig`，将 `DateTimeConverter` 合并到它自己生成的配置类中。

生成的 Mapper 仍引用 **Plus 自己的 Config**，不是直接引用公共配置：

```text
GlobalMapperConfig（提供 uses = DateTimeConverter.class）
    → Plus 生成的 AutoMapperConfig（合并公共 uses，类名可能带后缀）
        → Plus 生成的 Mapper
            → 调用自动配置注册的 DateTimeConverter
```

所以业务模型只需 `@AutoMapper(target = User.class)`，无需再填写 `uses`，也无需重复声明 Plus 全局配置。

运行期：Boot 通过 `AutoConfiguration.imports` 发现 `MapStructAutoConfiguration`，按外部时区或 JVM 默认时区注册转换器。业务自定义同类型 Bean 时，默认 Bean 自动退让。

依赖配置注入的映射使用 Spring 提供的 Converter，不使用 `new Converter()` 或 `Mappers.getMapper()` 绕开 Spring。

### 情况二：只使用普通 MapStruct

普通 MapStruct 不通过 Plus 的全局配置生成接口，需要在每个 Mapper 上显式引用 `GlobalMapperConfig`：

```java
import dev.macula.boot.mapstruct.GlobalMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {
    // UserForm.createTime 为 Date，User.createTime 为 LocalDateTime。
    User toEntity(UserForm form);
    UserForm toForm(User entity);
}
```

在 Spring 业务 Bean 中注入生成的 Mapper 后调用：

```java
@Autowired
private UserMapper userMapper;

User user = userMapper.toEntity(form);
UserForm result = userMapper.toForm(user);
```

这里 MapStruct 读取的是公共配置上的 `org.mapstruct.MapperConfig`，直接使用 Spring 组件模型和公共日期转换器。模型本身不需要 `@AutoMapper`。

```text
@Mapper(config = GlobalMapperConfig.class)
    → MapStruct 生成的 Mapper 实现
        → 调用自动配置注册的 DateTimeConverter
```

仅引入 Starter 或设置 `componentModel = "spring"`，不会自动给普通 Mapper 加上日期转换规则；省略 `config` 时，`Date` 与 `LocalDateTime` 仍按 MapStruct 内置的 UTC 规则转换。运行时自动配置只负责注册 Bean，不能替代编译期的 `config` 引用。

### 自定义或禁用

提供自己的 `DateTimeConverter` Bean 即可替换默认实现。仅禁用默认 Bean 可配置：

```yaml
macula:
  mapstruct:
    enabled: false
```

此开关不改写已生成的 Mapper；若这些 Mapper 使用日期转换器，禁用后需要自行提供 Bean，或在编译时改用不依赖它的配置。

已有业务 Plus 全局配置时，在业务 POM 指定其类名，替换 Parent 默认值：

```xml
<properties>
    <mapstruct-plus.mapper-config-class>com.example.ApplicationMappingConfig</mapstruct-plus.mapper-config-class>
</properties>
```

业务配置使用 `io.github.linpeilie.annotations.MapperConfig`。需要保留默认时间转换时，将 `DateTimeConverter.class` 合并到 `uses`；每个业务编译模块只定义一份 Plus 配置。

Parent 的编译参数优先于模块源码中的 Plus `@MapperConfig`，已有自定义配置必须同时覆盖上述 POM 属性，避免被默认配置替代。未依赖此 Starter、classpath 不存在默认配置类的模块，由 Plus 沿用原有配置。

### 编译配置

继承 `macula-boot-parent` 时已配置 Lombok、`mapstruct-plus-processor`、`lombok-mapstruct-binding` 和 `-Amapstruct.defaultComponentModel=spring`，无需重复配置；IDE 也需启用注解处理。

仅导入 BOM 不会继承构建插件，需自行配置注解处理器及 `-Amapstruct.plus.mapperConfigClass=dev.macula.boot.mapstruct.GlobalMapperConfig`。自动配置只能注册运行时 Bean，无法修改已经编译完成的映射实现。

## 验证

```bash
mvn -pl macula-boot-starters/macula-boot-starter-mapstruct -am test
```

测试覆盖自动配置发现、可选时区绑定、用户覆盖、禁用、classpath 条件、非法时区，并实际生成普通 MapStruct / MapStruct Plus Mapper；Plus 测试模型没有 `uses` 或模块配置，验证 Parent 的默认配置加载。覆盖Date/Instant双向往返、Instant纳秒精度和空值，不连接数据库或其他外部服务。

## 依赖引入

此 Starter 引入 `mapstruct-plus-spring-boot-starter`。MapStruct、MapStruct Plus 和注解处理器版本由 Macula Boot Parent 统一管理。

## 版权说明

- [MapStruct](https://github.com/mapstruct/mapstruct/blob/main/LICENSE.txt)
- [MapStruct Plus](https://github.com/linpeilie/mapstruct-plus/blob/main/LICENSE)
