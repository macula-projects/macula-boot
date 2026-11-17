## 概述

该模块主要使用alibaba druid作为数据源工具，mybatis-plus作为数据库操作框架

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-mybatis-plus</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

默认自动加载MyBatisPlusAutoConfiguration，已经开启了EnableTransactionManagement事务注解
可配置属性：

```yaml
macula:
  mybatis-plus:
    tenant-id: 1                            # 默认租户 ID，YAML 使用整数，不加 L
    tenant-suffixes: [tenant, TENANT]        # 需要租户隔离的表名后缀
    audit:
      create-by-name: createBy                # 创建人，默认createBy
      create-time-name: createTime            # 创建时间，默认createTime
      last-update-by-name: lastUpdateBy       # 最后更新人，默认lastUpdateBy
      last-update-time-name: lastUpdateTime   # 最后更新时间，默认lastUpdateTime
```

配置前缀为 `macula.mybatis-plus`；数据库连接使用 `spring.datasource.*`，MyBatis-Plus 原生选项使用 `mybatis-plus.*`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `tenant-enable` | `true` | 开启租户拦截器 |
| `tenant-id` | `1` | 默认租户 ID |
| `tenant-suffixes` | `[tenant, TENANT]` | 参与租户过滤的表名后缀 |
| `data-permission-enable` | `true` | 开启数据权限拦截器 |
| `max-limit` | `1000` | 分页单页记录数上限 |
| `audit.create-by-name` / `audit.create-time-name` | `createBy` / `createTime` | 创建人 / 创建时间的 Java 属性名 |
| `audit.last-update-by-name` / `audit.last-update-time-name` | `lastUpdateBy` / `lastUpdateTime` | 更新人 / 更新时间的 Java 属性名 |

## 核心功能

### 阿里druid数据源配置

数据源的配置请参考[druid-spring-boot-starter 官方文档](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)

### BaseEntity

默认提供了ID和审计字段，ID默认是自增长模式，如果数据库的默认字段不是标准的，请自行编写基础的Entity。同时记得在配置文件中设置审计字段的名称

```java

@Getter
@Setter
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @Schema(description = "更新人")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String lastUpdateBy;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastUpdateTime;

}
```

### 主键策略

@TableId注解用于设置主键字段，通过IdType设置采用的主键策略，具体可以参考[官方文档](https://baomidou.com/pages/223848/#tableid)

#### （1）自动增长（默认）

IdType.AUTO：使用数据库自增长ID

#### （2）数据库序列

IdType.INPUT： 如果要使用数据库的序列，要使用@KeySequence注解，同时要定义IKeyGenerator实现类并注册到Spring

#### （3）分配ID

IdType.ASSIGN_ID和ASSIGN_UUID：默认使用系统提供的雪花算法，如要自定义雪花算法，则需要实现IdentifierGenerator接口，并且注册到Spring，但是不能与IKeyGenerator同时注册。

### 逻辑删除

通过@TableLogic注解标识逻辑删除字段，具体可参考[官方文档](https://baomidou.com/pages/6b03c5/#%E4%BD%BF%E7%94%A8%E6%96%B9%E6%B3%95)

### 通用枚举

```yaml
mybatis-plus:
  type-enums-package: dev.macula.i18n-base.starter.mproot.test.entity.enums
```

要使用通用枚举，需要定义枚举类型，同时记得上述配置加入到你的应用配置中去，具体可参考[官方文档](https://baomidou.com/pages/8390a4/)

> **注意：从mybatis-plus 3.5.2开始已经无需上述配置了。**

### 复杂查询

默认情况下自定义SQL的XML配置文件放在src/main/resources/mapper/*.xml目录下，不要放在src/main/java目录下，有可能编译的时候会忽略xml等资
源文件。如果不是放在/mapper目录下，需要单独配置：

```yaml
mybatis-plus:
  mapper-locations: classpath:/dev/macula/**/mapper/*Mapper.xml
```

### 默认开启插件

#### 租户插件

```java
// 租户插件
interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler(){
@Override public Expression getTenantId(){

    // 只有已认证且非匿名的 ROOT 允许采用请求上下文中的租户 ID
    if(SecurityUtils.isRoot() && Objects.nonNull(TenantContextHolder.getCurrentTenantId())){
    return new LongValue(TenantContextHolder.getCurrentTenantId());
    }

    // 获取当前登录用户的租户上下文（aksk访问带租户信息）
    if(Objects.nonNull(SecurityUtils.getTenantId())){
    return new LongValue(SecurityUtils.getTenantId());
    }

    // 默认租户ID
    return new LongValue(properties.getTenantId());
    }

// 这是 default 方法,默认返回 false 表示所有表都需要拼多租户条件
@Override public boolean ignoreTable(String tableName){
    // 分租户的表名称应该统一以xx标识
    return!Arrays.stream(properties.getTenantSuffixes()).anyMatch(suffix->tableName.endsWith(suffix));
    }
    }));
```

- 已认证的 ROOT 优先从 TenantContextHolder 读取请求租户；非 ROOT 忽略请求租户，不返回 403，继续使用认证租户；
- 其次是从登录上下文获取租户ID，一般用于ak/sk访问应用的时候，通过应用配置获取租户ID；
- 最后是从应用配置的租户ID获取

#### 数据权限插件

在Mapper的方法上使用注解@DataPermission即可实现该方法按照用户角色关联的数据权限过滤数据。

```java

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @DataPermission(deptAlias = "u", userAlias = "u")
    Page<UserBO> listUserPages(Page<UserBO> page, UserPageQuery queryParams);
}
```

@DataPermission的定义和包含的属性如下：

```java

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DataPermission {
    // 部门ID所在表别名
    String deptAlias() default "";

    // 部门ID字段
    String deptIdColumnName() default "dept_id";

    // 用户ID所在表别名
    String userAlias() default "";

    // 用户ID字段
    String userIdColumnName() default "create_by";
}
```

数据权限范围定义如下：

```java
/**
 * {@code DataScopeEnum} 数据权限范围
 */
public enum DataScopeEnum implements IBaseEnum<Integer> {
    /**
     * value 越小，数据权限范围越大
     */
    ALL(0, "所有数据"), DEPT_AND_SUB(1, "部门及子部门数据"), DEPT(2, "本部门数据"), SELF(3, "本人数据"),
    DEFAULT(9, "默认范围");

    @Getter
    private Integer value;

    @Getter
    private String label;

    DataScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
```

从原理来说，实际上是通过实现DataPermissionHandler，对具体的SQL语句根据用户的数据权限重新按照部门ID或者用户ID生成新的SQL语句过滤数据。

SELF 使用 `create_by = ?`，用户名通过 JDBC 类型处理器绑定，不进入 SQL 文本；部门范围同样使用绑定的 Long 参数。字段名和别名只接受简单标识符。空用户名、空部门范围返回恒假条件；原 WHERE 与权限条件分别分组，保证 OR 分支受权限约束。缺失或 DEFAULT dataScope 的既有策略保持不变，非法范围报错。

默认使用 `ParameterizedDataPermissionInterceptor` 同步维护 SQL、`ParameterMapping` 和 `additionalParameters`，按最终 SQL 中的占位符顺序组合原参数与新增权限参数；UNION、子查询、foreach、分页及写操作不会简单地将新增参数追加到末尾。查询结束或异常时恢复嵌套参数上下文。

部门及子部门继续在原业务 SQL 中通过 `sys_dept(id, tree_path)` 子查询过滤，使用绑定参数的等值、前缀/中间/后缀 LIKE 匹配完整逗号分隔项，部门 12 不会匹配 112。该分支固定绑定 5 个条件参数，保留同一条 SQL 的执行和数据一致性边界。

```sql
dept_id IN (
    SELECT id FROM sys_dept
    WHERE id = ? OR tree_path = ?
       OR tree_path LIKE ? OR tree_path LIKE ? OR tree_path LIKE ?
)
```

部门值为 12 时，5 个绑定值依次为 `12L`、`"12"`、`"12,%"`、`"%,12,%"`、`"%,12"`。

升级自定义接入时：自行组合原生 `DataPermissionInterceptor(new MyDataPermissionHandler())` 的代码须迁移为 `new ParameterizedDataPermissionInterceptor()`；直接调用旧静态 `dataScopeFilter` 不能自行执行尚未绑定的值表达式。缺少参数上下文时明确报错，不回退为字符串拼接或无限制查询。自动配置的普通使用方无需改代码。

参数遍历覆盖 `TOP (?)`（包括 `PERCENT`、`WITH TIES`）及 Oracle `START WITH` / `CONNECT BY`（包括 `NOCYCLE` 和两种子句顺序），内部原参数仍与权限参数按最终 SQL 输出顺序绑定。参数映射不完整时拒绝执行，不回退为无限制查询。TOP 已在 H2 MSSQLServer 模式执行验证；Oracle 层级语法目前只验证解析改写与参数序列。

当前集成验证覆盖内存 H2 的五种模式，不能据此宣称所有厂商数据库或所有 SQL 语法均通过验证。

```bash
# 无外部服务：真实 MyBatis + H2 默认及多种兼容模式
mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am verify -Dit.test=DataPermissionDatabaseIT -Dfailsafe.failIfNoSpecifiedTests=false
```

真实 MySQL 回归测试 `MyDataPermissionMysqlIT` 仅在显式设置 `MACULA_SECURITY_TEST_MYSQL_URL`（`jdbc:mysql:` URL）、`MACULA_SECURITY_TEST_MYSQL_USER`、`MACULA_SECURITY_TEST_MYSQL_PASSWORD` 时使用独立测试库执行，不读取业务数据源；测试使用连接级随机临时表，连接关闭后自动销毁。未提供 URL 时该测试明确跳过。

```bash
mvn -pl macula-boot-starters/macula-boot-starter-mybatis-plus -am verify -Dit.test=MyDataPermissionMysqlIT -Dfailsafe.failIfNoSpecifiedTests=false
```


#### 分页插件

引入PaginationInnerInterceptor分页拦截器，返回IPage<T>接口，具体可参考[官方文档](https://baomidou.com/pages/97710a)

默认设置了最大返回记录数不能超过1000条。

在定义需要分页的API时，可以基于下面的类定义分页的属性，该基类用户查询VO请求入参，然后在Service实现中构造Mapper需要的Page实例。

```java
/**
 * {@code BasePageQuery} 带分页查询的基类
 *
 * @author rain
 * @since 2022/12/21 13:47
 */
@Data
@Schema
public class BasePageQuery {
    @Schema(description = "页码", example = "1")
    private int pageNum = 1;

    @Schema(description = "每页记录数", example = "10")
    private int pageSize = 10;
}

    @Override
    public IPage<UserVO> listUserPages(UserPageQuery queryParams) {

        // 查询数据
        Page<UserBO> userBoPage =
            this.baseMapper.listUserPages(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);

        // 实体转换
        return userConverter.bo2Vo(userBoPage);
    }
```

#### 乐观锁插件

引入OptimisticLockerInnerInterceptor乐观锁插件，更新具有@Version注解的实体时会自动加上version=xx条件，这种情况下需要判断update的返回值是否是>
=1或者是否是true，以判断更新是否成功，具体可参考[官方文档](https://baomidou.com/pages/0d93c0)

#### 防全表更新与删除插件

具体可参考[官方文档](https://baomidou.com/pages/f9a237)

#### 自动填充审计字段

默认的审计字段名称是createTime, lastUpdateTime, createBy, lastUpdateBy。数据库中的字段请转换为下划线。该名称有配置属性决定。
如果你先于MyBatisPlusAutoConfiguration定义MetaObjectHandler，则会使用你定义的类。

#### Duration类型映射

将数据库中的**BIGINT**或者**INTEGER**映射为**Duration**，数据库中的单位是秒。

#### 字段加解密

请参考 [Crypto章节](../macula-boot-starter-crypto/README.md)

## 依赖引入

```xml

<dependencies>
    <!-- macula-boot-commons -->
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>

    <!-- crypto -->
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-crypto</artifactId>
    </dependency>

    <!-- MySql -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Druid -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
    </dependency>

    <!-- MyBatis -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-security</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 版权说明

- mybatis-plus：https://github.com/baomidou/mybatis-plus/blob/3.0/LICENSE
- mysql-connector-java：https://github.com/mysql/mysql-connector-j/blob/release/8.0/LICENSE
