# DDD工程样例

## 关于DDD的目录结构说明

```text
    macula-cloud-system
        - interfaces        // 外部访问界面适配层
            - facade        // 存放给外部访问的Api类
            - listener      // 事件监听器
            - task          // 任务调度器的访问入口
        - app               // 应用服务层（负责领域服务的业务流程编排，不要把业务规则写在此处）
            - converter     // 将dto转为domain，或将domain转为dto
            - dto           // 应用层的对外数据传输对象
                -request    // 存放外部请求对象
                            // 根目录下存放返回对象，在interfaces层自动封装为Response
            - service       // 应用层服务，用于编排领域层服务。复杂查询服务可以直接访问infra层。
                            // 复杂业务走领域服务层，查询等没有明显领域特性的直接走infra
            - task          // 给定时任务访问的服务层
        - domain            // 领域模型层（按照聚合分包，基于DDD的思想设计模型，在实体中编写业务规则，如果跨实体则写在领域服务中）
            - xxx           // 某个聚合
                - entity    // 聚合实体，包含聚合根、值对象（不采用充血模型，持久化不写在实体类）
                - event     // 领域事件实体
                - repository// 领域实体持久化（以聚合根为维度持久化，一个聚合根一个类）
                - service   // 领域服务，多个实体编排，包含复杂实体的创建工厂
        - infra
            - repository    // 领域持久化实现
            - dataobj       // 数据对象，用于持久化、远程调用
            - mapper        // 数据库的操作
            - feign         // 远程RPC调用
```