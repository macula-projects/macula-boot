# 应用分层规范

> Macula 应用分层架构规范，支持经典三层架构和 DDD 架构。

## 经典三层架构 - 分层规约

### 展示层（Controller）

1. **【强制】** Controller 方法不要直接返回 Result 包裹类，框架会根据返回结果自动封装为 Result 结构返回
2. **【强制】** Controller 方法体里面不要编写业务逻辑，只做简单的 Service 方法编排
3. **【推荐】** Controller 方法参数建议添加 `@Validated` 注解，用于参数校验
4. **【强制】** Controller 方法不要吃掉异常，如果没有特殊需要，直接抛出服务层异常即可

### 业务逻辑层（Service）

1. **【强制】** Service 方法需要考虑事务，一个 Service 方法应该是事务一致的
2. **【强制】** Service 方法需要根据错误情况抛出对应异常，抛出的异常要继承自 `BizException`

### 数据存取层（Mapper/Repository）

1. **【推荐】** 建议使用 MyBatis-Plus 或者 JPA 框架
2. **【强制】** 不要通过拼接字符串的方式编写 SQL
3. **【强制】** 不要在 Java 代码中拼接 SQL

## 对象模型定义与使用场景

Macula 对各层对象明确定义其使用场景：

| 对象类型 | 使用场景 | 可穿透层次 |
| -------- | -------- | ---------- |
| **Query** | 查询对象，接收查询参数 | Controller → Service → Mapper |
| **Form/DTO** | 表单/传输对象，更新/创建时使用 | Controller → Service → ... |
| **VO** | 视图对象，返回给前端的结果 | Service → Controller → 前端 |
| **BO** | 业务对象，联合多表查询结果或多个 Entity 组合 | Mapper → Service → ... |
| **Entity** | 实体对象，与数据库表一一对应（等价于 PO） | Mapper → Service → ... |

### 对象命名规范

- 查询对象：`XXXQuery`
- 表单对象：`XXXForm`
- 业务对象：`XXXBO`
- 视图对象：`XXXVO`（有时简写为 `XXXVo`）
- 实体对象：`XXX`（直接使用业务名称）

### 对象转换

推荐使用 `mapstruct` 进行对象之间的转换，避免手动编写 get/set 代码。

示例转换器：
```java
@Mapper(componentModel = "spring")
public interface ApplicationConverter {
    ApplicationBO entity2Bo(Application entity);
    Application form2Entity(ApplicationForm appForm);
    Page<ApplicationVO> bo2Vo(Page<ApplicationBO> bo);
}
```

## 经典三层架构代码示例

### 实体类（Entity）

```java
// 基类
@Getter
@Setter
public abstract class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String lastUpdateBy;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastUpdateTime;
}

// 业务实体
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("application")
public class Application extends BaseEntity {
    private String applicationName;
    private String sk;
    private String homepage;
    private String manager;
    private String maintainer;
    private String mobile;
    private String code;
    private String accessPath;
    private boolean useAttrs;
    private String allowedAttrs;
}
```

### Mapper 接口

```java
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
    Page<ApplicationBO> listApplicationPages(IPage<Application> page, ApplicationPageQuery queryParams);
}
```

### Service 接口

```java
public interface ApplicationService extends IService<Application> {
    Page<ApplicationVO> listApplicationPages(ApplicationPageQuery queryParams);
    boolean saveApplication(ApplicationForm appForm);
    boolean updateApplication(Long appId, ApplicationForm appForm);
    boolean deleteApplications(String idsStr);
}
```

### Service 实现

```java
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> 
        implements ApplicationService {

    private final ApplicationConverter applicationConverter;

    @Override
    public Page<ApplicationVO> listApplicationPages(ApplicationPageQuery queryParams) {
        Page<Application> page = new Page<>(queryParams.getPageNum(), queryParams.getPageSize());
        Page<ApplicationBO> bo = this.baseMapper.listApplicationPages(page, queryParams);
        return applicationConverter.bo2Vo(bo);
    }

    @Override
    @Transactional
    public boolean saveApplication(ApplicationForm appForm) {
        Application application = applicationConverter.form2Entity(appForm);
        return this.save(application);
    }

    @Override
    @Transactional
    public boolean updateApplication(Long appId, ApplicationForm appForm) {
        Application application = applicationConverter.form2Entity(appForm);
        application.setId(appId);
        return this.updateById(application);
    }

    @Override
    @Transactional
    public boolean deleteApplications(String idsStr) {
        Assert.isTrue(StrUtil.isNotBlank(idsStr), "删除的应用数据为空");
        List<Long> ids = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return this.removeByIds(ids);
    }
}
```

### Controller

```java
@Tag(name = "应用管理", description = "应用管理")
@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "获取应用列表分页")
    @GetMapping
    public Page<ApplicationVO> listApplications(ApplicationPageQuery queryParams) {
        return applicationService.listApplicationPages(queryParams);
    }

    @Operation(summary = "新增应用")
    @PostMapping
    public boolean saveApplication(@Valid @RequestBody ApplicationForm formData) {
        return applicationService.saveApplication(formData);
    }

    @Operation(summary = "修改应用")
    @PutMapping(value = "/{appId}")
    public boolean updateApplication(@PathVariable Long appId, 
                                     @Valid @RequestBody ApplicationForm formData) {
        return applicationService.updateApplication(appId, formData);
    }

    @Operation(summary = "删除应用")
    @DeleteMapping("/{ids}")
    public boolean deleteApplications(@PathVariable("ids") String ids) {
        return applicationService.deleteApplications(ids);
    }
}
```

## DDD 架构

Macula 没有强制使用哪种分层架构，可根据具体场景和团队情况选择适合的分层架构。DDD 架构支持待完善。
