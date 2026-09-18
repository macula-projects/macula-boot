# Alibaba Provider 2 预留模块

该模块用于扩展 Alibaba 链路中的第二个独立服务提供方。当前只保留 Maven 模块边界，没有启动类或业务代码，Spring Boot Maven Plugin 也已设置为跳过。

如果新增实现，请使用独立的 `spring.application.name` 和端口，并同步补充本 README 中的依赖、启动方式和验证端点。
