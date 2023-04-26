# Macula Boot

该模块提供微服务应用开发的基础依赖库

```
macula-boot-parent                                   POM父模块，所有项目的parent
macula-boot-commons                                  通用模块，提供返回结构体、助手类、异常封装、常量等
macula-boot-docs                                     文档
macula-boot-starter
    01. macula-boot-starter-mapstruct                对象转换
    02. macula-boot-starter-crypto                   加解密模块
    03. macula-boot-starter-cache                    多级缓存
    04. macula-boot-starter-redis                    REDIS（基于Redisson）
    05. macula-boot-starter-lock4j                   分布式锁
    06. macula-boot-starter-idempotent               幂等
    07. macula-boot-starter-leaderelection           基于Redis的Leader选择器
    08. macula-boot-starter-mybatis-plus             MyBatis Plus
    09. macula-boot-starter-sender                   基于本地数据库发送MQ消息（先落库再异步发MQ）
    10. macula-boot-starter-rocketmq                 RocketMQ
    11. macula-boot-starter-liteflow                 可编排的组件引擎
    12. macula-boot-starter-statemachine             基于Spring StateMachine的状态机
    13. macula-boot-starter-security                 基于Token的安全校验
    14. macula-boot-starter-skywalking               Skywalking集成
    15. macula-boot-starter-springdoc                Swagger Doc v3
    16. macula-boot-starter-web                      WEB
    17. macula-boot-starter-feign                    Feign定制，使用OkHTTP
    18. macula-boot-starter-seata                    分布式事务
    19. macula-boot-starter-oss                      资源中心(minio/aliyun/cos/aws s3/本地)
    20. macula-boot-starter-tinyid                   ID生产
    21. macula-boot-starter-system                   接入菜单和权限等系统服务
    22. macula-boot-starter-powerjob                 基于PowerJob的定时任务（类似阿里云的ScheduleX）
    23. macula-boot-starter-dubbo                    基于Dubbo的RPC服务
    24. macula-boot-starter-cloud                    基于Spring Cloud的RPC服务
        1. macula-boot-starter-gateway               网关定制，基于Oauth2认证，权限控制也集中在网关
        2. macula-boot-starter-alibaba               基于alibaba spring cloud体系（nacos、sentinel、seata）
        3. macula-boot-starter-alibaba-scg           基于slibaba的gateway定制
        4. macula-boot-starter-tencent               基于tencent的polarismesh服务治理体系
        5. macula-boot-starter-tecent-scg            基于tencent的gateway定制
        6. macula-boot-starter-tsf                   基于TSF的服务治理体系
        7. macula-boot-starter-tsf-scg               基于TSF的网关定制
```