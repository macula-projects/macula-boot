## 概述

该模块主要使用alibaba druid作为数据源工具，Spring Data
JPA和Fenix作为数据库操作框架。[ Fenix](https://github.com/blinkfox/fenix)（菲尼克斯）是一个为了解决复杂动态 SQL (`JPQL`)
而生的 `Spring Data JPA` 扩展库，能辅助开发者更方便快捷的书写复杂、动态且易于维护的 SQL，支持 ActiveRecord 模式和多种查询方式。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-jpa</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

配置上没有太特殊的，主要是数据源配置以及fenix配置。

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/xxx?serverTimezone=Asia/Shanghai&characterEncoding=utf-8
    username: xxx
    password: xxx
  jpa:
    show-sql: true 		# 默认false，在日志里显示执行的sql语句
    hibernate:
    	# 指定为update，每次启动项目检测表结构有变化的时候会新增字段，表不存在时会 新建，如果指定create，
    	# 则每次启动项目都会清空数据并删除表，再新建。生产要指定为none
      ddl-auto: update 
      
# Fenix 的几个配置项、默认值及详细说明，通常情况下你不需要填写这些配置信息（下面的配置代码也都可以删掉）.
fenix:
  # v2.4.1 版本新增，表示是否开启 debug 调试模式，默认 false。
  # 当开启之后，对 XML 中的 SQL 会进行实时文件流的读取和解析，不需要重启服务。切记仅在开发环境中开启此功能.
  debug: true
  # 成功加载 Fenix 配置信息后，是否打印启动 banner，默认 true.
  print-banner: true
  # 是否打印 Fenix 生成的 SQL 信息，默认为空.
  # 当该值为空时，会读取 'spring.jpa.show-sql' 的值，为 true 就打印 SQL 信息，否则不打印.
  # 当该值为 true 时，就打印 SQL 信息，否则不打印. 生产环境不建议设置为 true.
  print-sql: true
  # 扫描 Fenix XML 文件的所在位置，默认是 fenix 目录及子目录，可以用 yaml 文件方式配置多个值.
  xml-locations: fenix
  # 扫描你自定义的 XML 标签处理器的位置，默认为空，可以是包路径，也可以是 Java 或 class 文件的全路径名
  # 可以配置多个值，不过一般情况下，你不自定义自己的 XML 标签和处理器的话，不需要配置这个值.
  handler-locations:
  # v2.2.0 新增的配置项，表示自定义的继承自 AbstractPredicateHandler 的子类的全路径名
  # 可以配置多个值，通常情况下，你也不需要配置这个值.
  predicate-handlers:
  # v2.7.0 新增的配置项，表示带前缀下划线转换时要移除的自定义前缀，多个值用英文逗号隔开，通常你不用配置这个值.
  underscore-transformer-prefix:
```

## 核心功能

### 阿里druid数据源配置

数据源的配置请参考[druid-spring-boot-starter 官方文档](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)

### BaseEntity

默认提供了ID和审计字段，ID默认是自增长模式，如果数据库的默认字段不是标准的，请自行编写基础的Entity。同时记得在配置文件中设置审计字段的名称

```java
@Getter
@Setter
@Accessors(chain = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<PK extends Serializable> implements Persistable<PK> {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    @Id
    private PK id;

    @Schema(description = "创建人")
    @CreatedBy
    private String createBy;

    @Schema(description = "更新人")
    @CreatedDate
    private LocalDateTime createTime;

    @Schema(description = "创建时间")
    private String lastUpdateBy;

    @Schema(description = "更新时间")
    private LocalDateTime lastUpdateTime;

    @Transient
    @Override
    public boolean isNew() {
        return null == getId();
    }
}
```

### Fenix快速开始

```java
public interface BlogRepository extends JpaRepository<Blog, String> {

    /**
     * 使用 {@link QueryFenix} 注解来演示根据散参数、博客信息Bean(可以是其它Bean 或者 Map)来多条件模糊分页查询博客信息.
     *
     * @param ids      博客信息 ID 集合
     * @param blog     博客信息实体类，可以是其它 Bean 或者 Map.
     * @param pageable JPA 分页排序参数
     * @return 博客分页信息
     */
    @QueryFenix
    Page<Blog> queryMyBlogs(@Param("ids") List<String> ids, @Param("blog") Blog blog, Pageable pageable);

}
```

```xml
<!-- 这是用来操作博客信息的 Fenix XML 文件，请填写 namespace 命名空间. -->
<fenixs xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        namespace="dev.macula.boot.starter.jpa.test.repository.BlogRepository"
        xmlns="http://macula.dev/schema/jpa"
        xsi:schemaLocation="http://macula.dev/schema/jpa https://macula.dev/schema/jpa/fenix.xsd">

    <!-- 这是一条完整的 Fenix 查询语句块，必须填写 fenix 标签的 id 属性. -->
    <fenix id="queryMyBlogs">
        SELECT
        b
        FROM
        Blog AS b
        WHERE
        <in field="b.id" value="ids" match="ids != empty"/>
        <andLike field="b.author" value="blog.author" match="blog.author != empty"/>
        <andLike field="b.title" value="blog.title" match="blog.title != empty"/>
        <andBetween field="b.createTime" start="blog.createTime" end="blog.lastUpdateTime"
                    match="(?blog.createTime != empty) || (?blog.lastUpdateTime != empty)"/>
    </fenix>
</fenixs>
```

Fenix具体使用请参考[官方文档](https://blinkfox.github.io/fenix/#/README)

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <exclusions>
            <exclusion>
                <groupId>com.zaxxer</groupId>
                <artifactId>HikariCP</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- fenix -->
    <dependency>
        <groupId>com.blinkfox</groupId>
        <artifactId>fenix-spring-boot-starter</artifactId>
    </dependency>

    <!-- MySql -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>

    <!-- Druid -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-security</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- For Test -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 版权说明

- spring-data-jpa： https://github.com/spring-projects/spring-data-jpa/blob/main/LICENSE.txt
- fenix： https://github.com/blinkfox/fenix/blob/develop/LICENSE