# API模块

各微服务对外接口的FeignClient定义以及与接口相关的dto定义

api模块的结构一般为：

```
xxxx-api
    |--api                              # 微服务接口，@FeignClient定义
    |----factory
    |----XXXFeignClient.java
    |--form                             # 界面层表单数据对象
    |--dto                              # 数据传输对象
    |--query                            # 查询条件
    |--vo                               # 返回结果对象
```