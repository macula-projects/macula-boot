## 概述

macula-cloud-seata是基于seata-server的服务端实现，以nacos作为注册和配置中心。同时提供starter，默认支持RestTemplate、FeignClient的分布式事务支持。

## 客户端接入

### 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-seata</artifactId>
    <version>${macula.version}</version>
</dependency>
```

### 使用配置

客户端加入如下配置

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-tx-group
  enable-auto-data-source-proxy: true			# 开启自动数据源代理，如果不使用AT模式，不要开启，默认为true
  saga:
  	enabled: true													# 开启saga自动配置，默认是false
  config:
    type: nacos
    nacos:
      serverAddr: 127.0.0.1:8848
      dataId: "seata.properties"					# 默认是seata.properties
      username: 'nacos'
      password: 'nacos'
  registry:
    type: nacos
    nacos:
      application: seata-server						# seata服务器的ID
      server-addr: 127.0.0.1:8848
      username: 'nacos'
      password: 'nacos'
```

在nacos的seata.properties中增加需要的如下[事务群组配置](https://seata.io/zh-cn/docs/user/configurations.html)

```properties
   service.vgroupMapping.order-service-tx-group=default					# 示例
   service.vgroupMapping.account-service-tx-group=default				# 示例
   service.vgroupMapping.business-service-tx-group=default			# 示例
   service.vgroupMapping.storage-service-tx-group=default				# 示例
```

> 另外，client.http.interceptor-enabled=false可以关闭默认的HTTP拦截

### 核心功能

具体可以参考[官方文档](http://seata.io/zh-cn/docs/overview/what-is-seata.html)，这里只简述一些基本概念和基本的运行测试。

#### 数据源支持

##### AT模式

AT模式支持的数据库有：MySQL、Oracle、PostgreSQL、 TiDB、MariaDB。

##### TCC模式

TCC模式不依赖数据源(1.4.2版本及之前)，1.4.2版本之后增加了TCC防悬挂措施，需要数据源支持。

##### Saga模式

Saga模式不依赖数据源。

##### XA模式

XA模式只支持实现了XA协议的数据库。Seata支持MySQL、Oracle、PostgreSQL和MariaDB。

#### 注解开启全局事务

```java
@GetMapping(value = "testCommit")
@GlobalTransactional
public Object testCommit(@RequestParam(name = "id",defaultValue = "1") Integer id,
    @RequestParam(name = "sum", defaultValue = "1") Integer sum) {
    Boolean ok = productService.reduceStock(id, sum);
    if (ok) {
        LocalDateTime now = LocalDateTime.now();
        Orders orders = new Orders();
        orders.setCreateTime(now);
        orders.setProductId(id);
        orders.setReplaceTime(now);
        orders.setSum(sum);
        orderService.save(orders);
        return "ok";
    } else {
        return "fail";
    }
}
```

#### Seata AT模式

AT模式根据其名称也能反馈出来他的特性，他是自动型的[分布式事务解决方案](https://so.csdn.net/so/search?q=分布式事务解决方案&spm=1001.2101.3001.7020)
。这个自动提现在他无需代码入侵，也就是说我们不需要再编写多余的代码来实现这个模式，只需要在方法中添加上指定的注解即可。

那么AT模式是如何实现无代码入侵的呢？他的工作原理是什么？

完整内容可参考[原创博客](https://blog.51cto.com/u_15952602/6034758)

##### AT模式原理总览

AT模式分成两阶段来工作，我们先省略部分细节来整体了解其过程：

- 一阶段：

（1）拦截并解析业务SQL，找到需要在数据表中更新的数据，将其转换为undo_log，并且保存到提前在每个数据库中创建的undo_log表中。打个比方，如果你是在做`update product set price = 20 where price=10 and id=1`
的操作，那么undo_log中保存的就是反向sql `update product set price=10 where price=20 and id=1`。当然它的存储不会直接这么存，会经过处理。

（2）然后执行业务SQL，这时你会发现数据库中的数据是发生变化了的，同时undo_log中也有对应的新增数据

- 二阶段

（1）因为第一阶段已经提交了本地事务，数据已经更新过了，这个时候如果没有报错，那么直接删除掉undo_log以及行锁的数据即可

（2）但是如果发生了报错，就只需要根据undo_log来回退数据

![在这里插入图片描述](../images/seata_at_01.png)

这个就是AT模式执行的两阶段的整体视角，我们可以体会到的是AT模式下，自动帮助我们生成了undo_log、一阶段、二阶段的提交都是由seata完成的，并不需要我们写代码来实现，所以它的无代码入侵体现在这里。

##### AT模式应用场景

AT模式是实际开发中最常用的模式，他无代码入侵的特性，适应于不希望对代码进行改造的场景，因为AT模式要求数据库是支持本地事务的数据库，但是因为市面上多数使用的都是支持事务的数据库，所以其应用也十分广泛。

同时其性能也还不错，完全满足普通业务，如果对于性能有较高要求的，就要用到我们其他模式的。

AT应用场景总结：

- 不希望对代码进行改造
- 数据库支持事务操作
- 对性能没有特别高的要求

#### Seata TCC模式

TCC模式，全称Try-Confirm-Cancel，通过名称也能看出来其流程主要有三个步骤：

- 预处理 Try：实现业务检查和资源预留
- 确认/提交 Confirm：业务确认和提交
- 撤销/回滚 Cancel：业务回滚

![在这里插入图片描述](../images/seata_tcc_01.png)

理解TCC模式的关键在于理解Try-Confirm阶段，其中Try用来实现业务检查和资源预留，这个概念比较抽象，我们举个例子来看看：

> 现在我们有一个下订单的操作，订单创建后需要扣减商品库存

​ 完整内容可参考[原创博客](https://blog.51cto.com/u_15952602/6034752?articleABtest=0)

##### 案例

###### 订单服务

注意需要在接口上添加`@LocalTCC`注解

订单新增方法上要添加`@TwoPhaseBusinessAction`注解，并且声明confirm,cancel方法

```java
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.orderserver.entity.Order;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * @author whx
 * @date 2022/4/30
 */
@LocalTCC
public interface IOrderTccService extends IService<Order> {

    /**
     * @TwoPhaseBusinessAction 描述⼆阶段提交
     * name: 为 tcc⽅法的 bean 名称，需要全局唯⼀，⼀般写⽅法名即可
     * commitMethod: Commit/Confirm⽅法的⽅法名
     * rollbackMethod:Rollback/Cancel⽅法的⽅法名
     * @BusinessActionContextParamete 该注解可以将参数传递给声明的commitMethod和rollbackMethod，通过BusinessActionContext 获取。
     */
    @TwoPhaseBusinessAction(name = "addOrder", commitMethod = "addCommit", rollbackMethod = "addRollBack")
    void addOrder(@BusinessActionContextParameter(paramName = "order") Order order);

    boolean addCommit(BusinessActionContext context);

    boolean addRollBack(BusinessActionContext context);
}
```

实现类

```java
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.orderserver.entity.Order;
import com.example.orderserver.feign.ProductApi;
import com.example.orderserver.mapper.OrderMapper;
import com.example.orderserver.service.IOrderTccService;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author whx
 * @date 2022/4/30
 */
@Slf4j
@Service
@AllArgsConstructor
public class IOrderTccServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderTccService {

    // 商品feign调用接口
    private final ProductApi productApi;

    @Override
    public void addOrder(Order order) {
        // 设置状态为未提交
        order.setStatus(0);
        // 新增订单
        this.save(order);
        // 扣减库存
        productApi.reduceInventory(order.getProduct().getId(), order.getProduct().getFrozenInventory());
    }

    @Override
    public boolean addCommit(BusinessActionContext context) {
        Order order = JSON.parseObject(context.getActionContext("order").toString(), Order.class);
        int count = this.count(Wrappers.<Order>lambdaQuery().eq(Order::getId, order.getId()));
        if (count > 0) {
            order.setStatus(1);
            baseMapper.updateById(order);
        }
        log.info("事务 xid=" + context.getXid() + " 提交成功");
        return true;
    }

    @Override
    public boolean addRollBack(BusinessActionContext context) {
        Order order = JSON.parseObject(context.getActionContext("order").toString(), Order.class);
        int count = this.count(Wrappers.<Order>lambdaQuery().eq(Order::getId, order.getId()));
        if (count > 0) {
            // 删除数据
            this.removeById(order.getId());
        }
        log.info("事务 xid=" + context.getXid() + " 回滚成功");
        return true;
    }
}
```

###### 商品服务

```java
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.productserver.entity.Product;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * @author whx
 * @date 2022/4/30
 */
@LocalTCC
public interface IProductTccService extends IService<Product> {

    /**
     * @TwoPhaseBusinessAction 描述⼆阶段提交
     * name: 为 tcc⽅法的 bean 名称，需要全局唯⼀，⼀般写⽅法名即可
     * commitMethod: Commit/Confirm⽅法的⽅法名
     * rollbackMethod: Rollback/Cancel⽅法的⽅法名
     * @BusinessActionContextParamete 该注解可以将参数传递给声明的commitMethod和rollbackMethod，通过BusinessActionContext 获取。
     */
    @TwoPhaseBusinessAction(name = "reduceInventory", commitMethod = "reduceCommit", rollbackMethod = "reduceRollBack")
    String reduceInventory(@BusinessActionContextParameter(paramName = "productId") Long id,
        @BusinessActionContextParameter(paramName = "number") Integer number);

    boolean reduceCommit(BusinessActionContext context);

    boolean reduceRollBack(BusinessActionContext context);

}
```

实现类

```java
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.productserver.entity.Product;
import com.example.productserver.mapper.ProductMapper;
import com.example.productserver.service.IProductTccService;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author whx
 * @date 2022/4/30
 */
@Slf4j
@Service
public class IProductTccServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductTccService {

    /**
     * Transactional 添加行锁，防止多线程下更新库存时被其他线程读取到脏数据，这样能保证第一次获取时FrozenInventory为0
     * @param productId
     * @param number
     * @return
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String reduceInventory(Long productId, Integer number) {
        Product product = this.getOne(
            Wrappers.<Product>lambdaQuery().select(Product::getId, Product::getInventory, Product::getFrozenInventory)
                .eq(Product::getId, productId));
        // 当前可用库存需要排除历史冻结库存
        if (product.getInventory() >= number) {
            // 更新冻结库存
            product.setFrozenInventory(number);
            this.updateById(product);
            return "库存更新成功";
        }
        return "商品库存不足";

    }

    @Override
    public boolean reduceCommit(BusinessActionContext context) {
        Product product = this.getOne(
            Wrappers.<Product>lambdaQuery().select(Product::getId, Product::getInventory, Product::getFrozenInventory)
                .eq(Product::getId, context.getActionContext("goodsId")));
        Integer number = Integer.parseInt(context.getActionContext("number").toString());
        if (product != null) {
            //扣减库存
            product.setInventory(product.getInventory() - number);
            product.setFrozenInventory(0);
            this.saveOrUpdate(product);
        }
        log.info("事务 xid=" + context.getXid() + " 提交成功");
        return true;
    }

    @Override
    public boolean reduceRollBack(BusinessActionContext context) {
        Product product = this.getOne(
            Wrappers.<Product>lambdaQuery().select(Product::getId, Product::getFrozenInventory)
                .eq(Product::getId, context.getActionContext("goodsId")));
        Integer number = Integer.parseInt(context.getActionContext("number").toString());
        if (product != null) {
            // 恢复冻结库存
            product.setFrozenInventory(0);
            this.saveOrUpdate(product);
        }
        log.info("事务 xid=" + context.getXid() + " 回滚成功");
        return true;
    }
}
```

##### TCC模式的补偿措施

###### 重试机制

我们上述的代码中在confirm,cancel方法中实现了事务的提交和回滚，但是因为是我们自己通过代码实现的，所以还需要考虑一个问题：执行失败后的重试机制。当我们的confirm或者cancel方法也出现报错时，为了保证事务的最终一致性，我们应当做好重试机制处理：比如将数据发送到MQ，然后再进行接收处理。

###### 幂等性问题

所谓幂等就是操作一次和操作多次的执行效果是一样的。想象一下，我们的库存扣除操作，如果因为某一步操作报错，导致需要回滚重试，结果每次重试都会重复扣减库存，那这样肯定是不对的。所以为了保证我们在confirm,cancel中进行的重试机制不会使得我们的资源发生重复消耗，那么需要我们对方法做好幂等性处理：比如说通过添加状态字段来判断是否执行过

###### 悬挂问题

所谓悬挂问题，就是二阶段模式中，cancel比try先执行。这是怎么导致的呢？

就拿我们上述的案例来假设，在订单服务中调用商品服务的扣减库存方法reduceInventory时，因为是通过RPC(feign)
的方式来调用的，那么如果调用时刚好网络堵塞，或者商品服务出现问题，导致调用失败，出现报错，TM会通知TC出现错误，TC会通知所有的RM进行本地事务回滚，也就是执行cancel方法。当cancel方法执行完成后，try方法偏偏连通了，又执行了，那么就出现了问题，订单会被更新为未提交，但因为事务已经被cancel过了，就不会再执行confirm，也就没有谁再来将资源状态从预处理更新为已处理了。资源就会导致浪费。这时进行的回滚操作其实并没有真实回滚业务，这个现象我们称之为空回滚。所有我们需要针对悬挂问题进行防悬挂处理，方案呢就是限制如果二阶段执行完成，一阶段就不能再执行。比如执行cancel方法时会判断是否是空回滚，出现空回滚注册事务ID，try方法执行前先检查事务ID是否存在，如果存在则不允许执行当然这些处理呢，seata已经帮我们实现了，这也是使用现成的分布式事务框架的好处。省心！但是我们自己要知道这些问题和原理。

seata中的解决方案是增加一个事务记录表，在cancel阶段最后往事务记录表中插入一条记录（xid-status）标记cancel阶段已经执行过。此时try阶段进入时发现已经执行过回滚操作，则放弃try阶段的执行。

#### Seata Saga模式

Saga的定义是“长时间活动的事务”，是普林斯顿大学教授Hector & Kenneth发表的论文《sagas》中提出的概念。它的思想是允许分布式事务在全部提交前提前释放占用的某些资源。

完整内容可以参考[原创博客](https://blog.51cto.com/u_15952602/6034754?articleABtest=0)
，代码示例可参考[seata-samples](https://github.com/seata/seata-samples/blob/master/saga/README.MD)

Saga模式依然要求我们自己实现正向服务和补偿服务。但是它于TCC模式的区别之处在于：

- Saga的模式设计使得它天然适合于长流程的业务。TCC要实现同样的长流程的话，需要多写一个confirm操作，并且要考虑如何将业务拆分为两部分
- Saga模式在正向服务中时就已经提交了本地事务了，而补偿事务也比较好实现，将正向服务的结合逆向补偿即可。
- 比如正常服务是`update product set price=20 where price=30 and id=1`;
  那么补偿服务就是`update product set price=30 where price=20 and id=1`;
-
比起TCC模式，Saga模式更适用于一些老服务、第三方服务或者其他无法改造的服务，要接入到我们的分布式事务中时，就可以将其作为一个正向服务存在，而直接实现他的补偿服务即可。而TCC因为要对业务进行拆分为try-confirm-cancel，所以它不适用于不可改造的服务

同时，Saga模式同样不需要全局锁，只需要结合本地事务加本地锁即可，所以性能依旧有保证。

##### SAGA模式的三种事务类型

###### 可补偿性事务

所谓可补偿性事务，也就是可以使用、需要使用补偿事务来回滚数据的事务

比如说下订单，就需要删除订单的补偿事务，因此下订单就是一个可补偿性事务

###### 关键性事务

```
关键性事务是Saga执行的关键点，如果关键性事务运行成功，则Saga将一直运行到完成。关键性事务不一定是个可补偿性事务或者可重复性事务，但是他可以是最后一个可补偿的事务或第一个可重复的事务——《微服务架构设计模式》
```

通过书中的描述，我们知道关键性事务的定义从结构上理解，是处于可补偿性事务和可重复性事务的中间。

具体把哪个事务定义为关键性事务，还要根据具体的业务情况而定，我们可以通过以下标准来判断

- 从结构上是否处于可补偿事务和可重复事务之间
- 从业务上该事务是否能表示整个业务执行成功的转折点

###### 可重复性事务

关键性事务之后的事务就是可重复性事务，不需要回滚，并且保证能够执行完成。所以我们会通过一些机制来保证这类事务一定能执行成功，比如重试机制。

##### SAGA模式的补偿措施

与TCC模式类似，SAGA模式也涉及到如下几个问题

###### 幂等性问题

所谓幂等就是操作一次和操作多次的执行效果是一样的。

想象一下，我们的库存扣除操作，如果因为某一步操作报错，导致需要回滚重试，结果每次重试都会重复扣减库存，那这样肯定是不对的。

所以为了保证我们在confirm,cancel中进行的重试机制不会使得我们的资源发生重复消耗，那么需要我们对方法做好幂等性处理：

比如说通过添加状态字段来判断是否执行过。当然这一点在seata等分布式框架中不用我们再手动实现，框架已经帮我们实现了。

###### 悬挂问题

所谓悬挂问题，就是二阶段模式中，二阶段比一阶段先执行

这是怎么导致的呢？

我们拿下订单扣减库存的案例来说，在订单服务中调用商品服务的扣减库存方法reduceInventory时，通常通过RPC(feign)
的方式来调用，那么如果调用时刚好网络堵塞，或者商品服务出现问题，导致调用失败，出现报错，TM会通知TC出现错误，TC会通知所有的RM进行本地事务回滚，也就是执行补偿事务。

当补偿事务方法执行完成后，正向事务方法偏偏连通了，又执行了，那么就出现了问题，这个正向服务之前的补偿事务都执行了，但又执行了一个多余的正向服务。

所有我们需要针对悬挂问题进行防悬挂处理，方案呢就是限制如果二阶段执行完成，一阶段就不能再执行。

seata中的解决方案是增加一个事务记录表，在补偿服务执行后往事务记录表中插入一条记录（xid-status）标记补偿服务已经执行过。此时正向服务进入时发现已经执行过回滚操作，则放弃正向服务的执行。

###### SAGA模式应用场景

- 适用于长事务业务场景
- 适用于需要接入老服务、第三方服务或者其他无法改造的服务的业务场景
- 需要操作更细分散在多个服务、系统中的数据的业务场景

##### Seata XA模式

使用较少，具体参考https://blog.51cto.com/u_15952602/6034750?articleABtest=0

#### 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>io.seata</groupId>
        <artifactId>seata-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-feign</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-web</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 服务端介绍

请参考[官网](https://seata.io/)

## 版权说明

- seata：https://github.com/seata/seata/blob/2.x/LICENSE