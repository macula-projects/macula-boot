## 概述

macula-cloud-tinyid是用Java开发的一款分布式id生成系统，基于数据库号段算法实现，关于这个算法可以参考[美团leaf](https://tech.meituan.com/MT_Leaf.html)
或者[tinyid原理介绍](https://github.com/didi/tinyid/wiki/tinyid原理介绍)。Tinyid扩展了leaf-segment算法，支持了多db(master)
，tinyid是服务端，同时我们提供了starter(Java SDK)，获得了更好的性能与可用性。

## 客户端接入

### 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-tinyid</artifactId>
    <version>${macula.version}</version>
</dependency>
```

### 使用配置

```yaml
macula:
  cloud:
    tinyid:
      server: localhost:9000 # 可以通过逗号隔开多个服务端IP
      token: 0f673adf80504e2eaa552f5d791b644c
      connectTimeout: 5000
      readTimeout: 5000
```

### 核心功能

#### 性能与可用性

#### 性能

1. http方式访问，性能取决于http server的能力，网络传输速度
2. java-client方式，id为本地生成，号段长度(step)越长，qps越大，如果将号段设置足够大，则qps可达1000w+

#### 可用性

1. 依赖db，当db不可用时，因为server有缓存，所以还可以使用一段时间，如果配置了多个db，则只要有1个db存活，则服务可用
2. 使用tiny-client，只要server有一台存活，则理论上可用，server全挂，因为client有缓存，也可以继续使用一段时间

### 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 服务端介绍

### Tinyid的特性

1. 全局唯一的long型id
2. 趋势递增的id，即不保证下一个id一定比上一个大
3. 非连续性
4. 提供http和java client方式接入
5. 支持批量获取id
6. 支持生成1,3,5,7,9...序列的id
7. 支持多个db的配置，无单点

{{% alert title="提示" color="primary" %}}

适用场景:只关心id是数字，趋势递增的系统，可以容忍id不连续，有浪费的场景
不适用场景:类似订单id的业务(因为生成的id大部分是连续的，容易被扫库、或者测算出订单量)

{{%/alert%}}

### 推荐使用方式

- tinyid-server推荐部署到多个机房的多台机器
    - 多机房部署可用性更高，http方式访问需使用方考虑延迟问题
- 推荐使用tinyid-client来获取id，好处如下:
    - id为本地生成(调用AtomicLong.addAndGet方法)，性能大大增加
    - client对server访问变的低频，减轻了server的压力
    - 因为低频，即便client使用方和server不在一个机房，也无须担心延迟
    - 即便所有server挂掉，因为client预加载了号段，依然可以继续使用一段时间 注:
      使用tinyid-client方式，如果client机器较多频繁重启，可能会浪费较多的id，这时可以考虑使用http方式
- 推荐db配置两个或更多:
    -
    db配置多个时，只要有1个db存活，则服务可用 [多db配置](https://github.com/didi/tinyid/wiki/Tinyid-server-config#多db配置)
    ，如配置了两个db，则每次新增业务需在两个db中都写入相关数据

### Tinyid的原理

- tinyid是基于数据库发号算法实现的，简单来说是数据库中保存了可用的id号段，tinyid会将可用号段加载到内存中，之后生成id会直接内存中产生。
- 可用号段在第一次获取id时加载，如当前号段使用达到一定量时，会异步加载下一可用号段，保证内存中始终有可用号段。
- (如可用号段1~1000被加载到内存，则获取id时，会从1开始递增获取，当使用到一定百分比时，如20%(默认)
  ，即200时，会异步加载下一可用号段到内存，假设新加载的号段是1001~2000,则此时内存中可用号段为200~1000,1001~2000)
  ，当id递增到1000时，当前号段使用完毕，下一号段会替换为当前号段。依次类推。

### tinyid系统架构图

![Code架构](../images/tinyid.png)

下面是一些关于这个架构图的说明:

- nextId和getNextSegmentId是tinyid-server对外提供的两个http接口
- nextId是获取下一个id，当调用nextId时，会传入bizType，每个bizType的id数据是隔离的，生成id会使用该bizType类型生成的IdGenerator。
- getNextSegmentId是获取下一个可用号段，tinyid-client会通过此接口来获取可用号段
- IdGenerator是id生成的接口
- IdGeneratorFactory是生产具体IdGenerator的工厂，每个biz_type生成一个IdGenerator实例。通过工厂，我们可以随时在db中新增biz_type，而不用重启服务
-
IdGeneratorFactory实际上有两个子类IdGeneratorFactoryServer和IdGeneratorFactoryClient，区别在于，getNextSegmentId的不同，一个是DbGet,一个是HttpGet
-
CachedIdGenerator则是具体的id生成器对象，持有currentSegmentId和nextSegmentId对象，负责nextId的核心流程。nextId最终通过AtomicLong.andAndGet(
delta)方法产生。

## 版权说明

- tinyid：https://github.com/didi/tinyid/blob/master/LICENSE