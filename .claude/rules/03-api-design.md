# API 设计规范

> RESTful API 设计规约。

## 设计原则

### API 根 URL

- 如果预期系统非常庞大，建议尽量将 API 部署到独立专用子域名（例如 `api.`）下
- 如果确定 API 很简单，不会进一步扩展，则可以考虑放到应用根域名下面（例如 `/api/`）
- 微服务平台一般通过网关统一对外：`https://example.org/[xxx]/api/v1/*`

### API 各模块 URL

建议每个领域或者 Controller 统一一个 URL 根，形式为：`/api/v1/[xxx]`，xxx 表示某个领域模块名称。

### URI 设计规则

1. **URI 末尾不要添加 `/`**
   - 负面：`http://api.canvas.com/api/v1/shapes/`
   - 正面：`http://api.canvas.com/api/v1/shapes`

2. **禁止在 URL 中使用 `_`**，使用连字符 `-` 替代
   - 负面：`http://api.example.com/api/v1/blogs/my_first_post`
   - 正面：`http://api.example.com/api/v1/blogs/my-first-post`

3. **禁止使用大写字母**，全部使用小写，单词用 `-` 连接
   - 负面：`http://api.example.com/api/v1/My-Folder/My-Doc`
   - 正面：`http://api.example.com/api/v1/my-folder/my-doc`

4. **不要在 URI 中包含扩展名**，鼓励使用 HTTP Accept header 选择格式
   - 负面：`http://api.example.com/api/v1/my-doc/hello.json`
   - 正面：`http://api.example.com/api/v1/my-doc/hello`

5. **建议 URI 中的名称使用复数**
   - 负面：`http://api.college.com/api/v1/student/3248234`
   - 正面：`http://api.college.com/api/v1/students/3248234`

   处理关联关系示例：
   ```
   http://api.college.com/api/v1/students/3248234/courses
   http://api.college.com/api/v1/students/3248234/courses/physics
   ```

6. **建议 URI 设计时只包含名词**，动词由 HTTP 方法表示
   - 负面：`http://api.example.com/api/v1/get-all-employees`
   - 正面：`http://api.example.com/api/v1/employees`

### HTTP 动词使用

| 动词 | 含义 |
| ---- | ---- |
| **GET** | 从服务器取出资源（一项或多项） |
| **POST** | 在服务器新建一个资源 |
| **PUT** | 在服务器更新资源（客户端提供改变的完整资源） |
| **PATCH** | 在服务器更新资源（客户端提供改变的属性） |
| **DELETE** | 从服务器删除资源 |

### 分页与分页大小

- **【强制】** 禁止由第三方开发人员任意指定约束条件
- **【强制】** 服务器端必须设置默认单页数量，禁止无限制返回
- 建议默认每页数量不超过 100 条

## 版本管理

### 整体建议

- 建议通过 URI 指定服务版本，版本采用字符 `v` + 数字主版本号，例如 `/v1/xxxs`
- 建议版本控制在资源层面，也即 Controller 维度

### 分包示例

```
com.x.[user].domain.entity
com.x.api.[user].controller.v1
com.x.api.[user].controller.v2
```

### API 升级建议

1. 尽量做兼容性设计，不要随便升级版本
2. 升级可分步实施，不同版本相互独立，便于后续拆分独立部署、独立维护
3. 先将资源的部分方法升级至高版本，并进行灰度发布
4. 升级资源的所有方法至高版本，并发布公告要求客户限期升级
5. 停止旧版本服务

### 版本指定方式对比

- **URI 中指定版本**：`https://api.example.org/api/v1/users` - 直观，可直接在浏览器查看，推荐
- **参数中指定版本**：`https://api.example.org/api/users?v=1.0` - 破坏 REST HATEOAS 规则，不推荐

## 接口注释

必须使用 OpenAPI (Swagger) 规范注释接口：

```java
@Tag(name = "应用管理", description = "应用管理")
@RestController
@RequestMapping("/api/v1/app")
public class ApplicationController {

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
}
```

## 响应格式

Macula 框架自动将返回结果包装为 `Result` 对象，格式如下：

```json
{
  "success": true,
  "code": "00000",
  "msg": "成功",
  "data": {
    // 实际数据
  }
}
```

Controller 不需要手动包装，直接返回数据即可，框架会自动处理。
