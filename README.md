<h2 align="center">Macula Boot</h2>

<p align="center">
	<strong>基于Spring Boot、Spring Cloud Alibaba的微服务开发框架</strong>
</p>

<p align="center">
    <a href="https://github.com/macula-projects/macula-boot/blob/main/LICENSE" target="_blank">
        <img src="https://img.shields.io/github/license/macula-projects/macula-boot.svg" >
    </a>
    <a href="https://github.com/macula-projects/macula-boot/actions/workflows/snapshot.yml">
      <img src="https://img.shields.io/github/actions/workflow/status/macula-projects/macula-boot/snapshot.yml?branch=main&logo=github&logoColor=white" >
    </a>
    <a href="https://central.sonatype.com/search?q=macula&smo=true" target="_blank">
        <img src="https://img.shields.io/maven-central/v/dev.macula.boot/macula-boot-starters" />
    </a>
    <a>
        <img src="https://img.shields.io/badge/JDK-1.8+-green.svg" >
    </a>
    <a>
        <img src="https://img.shields.io/badge/SpringBoot-2.7+-green.svg" >
    </a>
    <a>
        <img src="https://img.shields.io/badge/SpringCloud-2021.x+-green.svg" >
    </a>
</p>

## 概述

### 平台简介

Macula是一个微服务应用开发平台，主要包括三大部分：MaculaBoot、MaculaCloud和MaculaCloudAdmin。

- MaculaBoot是微服务应用开发所需要的开发框架（如SpringCloudAlibaba、SpringCloudTecent等），基于众多开源产品进行甄选后轻度封装而成。
- MaculaCloud是一个微服务架构的通用技术服务体系，一方面可集成对接各大云厂商的微服务治理体系（如腾讯云TSF、阿里云EDAS、百度云CNAP等），
  另一方面提供大量内置可复用的通用技术服务（如系统管理、消息推送、资源存储、ID生成、任务调度等）。
- MaculaCloudAdmin是基于VUE的前端项目，与MaculaCloud配套，提供管理界面功能。使用Macula进行微服务架构的应用平台开发，一方面可以统一技术栈，降低管理与维护成本；另一方面可以避免重复造轮子，提升开发效率。

### 整体架构

Macula的整体架构如下图所示：

![image](https://macula.dev/img/structure-diagram.png)

## 主要功能

### Macula Boot
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
    09. macula-boot-starter-jpa                      Spring Data JPA集成
    10. macula-boot-starter-sender                   基于本地数据库发送MQ消息（先落库再异步发MQ）
    11. macula-boot-starter-rocketmq                 RocketMQ
    12. macula-boot-starter-liteflow                 可编排的组件引擎
    13. macula-boot-starter-statemachine             基于Spring StateMachine的状态机
    14. macula-boot-starter-security                 基于Token的安全校验
    15. macula-boot-starter-springdoc                Swagger Doc v3
    16. macula-boot-starter-web                      WEB
    17. macula-boot-starter-feign                    Feign定制，使用OkHTTP
    18. macula-boot-starter-seata                    分布式事务
    19. macula-boot-starter-oss                      资源中心(minio/aliyun/cos/aws s3/本地)
    20. macula-boot-starter-tinyid                   ID生产
    21. macula-boot-starter-system                   接入菜单和权限等系统服务
    22. macula-boot-starter-task                     基于xxljob等的定时任务（类似阿里云的ScheduleX）
    23. macula-boot-starter-retry                    重试框架
    24. macula-boot-starter-dubbo                    基于Dubbo的RPC服务
    25. macula-boot-starter-cloud                    基于Spring Cloud的RPC服务
        1. macula-boot-starter-gateway               网关定制，基于Oauth2认证，权限控制也集中在网关
        2. macula-boot-starter-alibaba               基于alibaba spring cloud体系（nacos、sentinel、seata）
        3. macula-boot-starter-alibaba-scg           基于slibaba的gateway定制
        4. macula-boot-starter-tencent               基于tencent的polarismesh服务治理体系
        5. macula-boot-starter-tecent-scg            基于tencent的gateway定制
        6. macula-boot-starter-tsf                   基于TSF的服务治理体系
        7. macula-boot-starter-tsf-scg               基于TSF的网关定制
```

### Macula Cloud

详见[Macula Cloud](https://github.com/macula-projects/macula-cloud)项目

### Macula Cloud

详见[Macula Cloud Admin](https://github.com/macula-projects/macula-cloud-admin)项目

## 编译说明

编译的时候需要指定deploy这个profile，否则revision不会处理

```shell
mvn clean install -DskipTests=true -Pdeploy
```

## 技术原理

Macula整体的技术交互与应用原理如下图所示：

![image](https://macula.dev/docs/00_introduce/%E6%A6%82%E8%BF%B0/images/macula-tech-diagram.png)


> 说明：Macula特别适用于多产品线（一个产品线对应一个应用平台）的研发团队。

## License

Macula Boot and Macula Cloud is Open Source software released under the Apache 2.0 license.