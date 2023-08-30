## 概述

基于[alibab cola statemachine](https://github.com/alibaba/COLA/tree/master/cola-components/cola-component-statemachine)
的状态机模块，cola statemachine是简单、轻量、性能极高的状态机DSL实现，解决业务中的状态流转问题。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-statemachine</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 核心功能

### COLA状态机介绍

COLA状态机是在Github开源的，作者也写了介绍文章：https://blog.csdn.net/significantfrank/article/details/104996419。

首先，状态机的实现应该可以非常的轻量，最简单的状态机用一个Enum就能实现，基本是零成本。其次，使用状态机的DSL来表达状态的流转，语义会更加清晰，会增强代码的可读性和可维护性。

开源状态机太复杂：就我们的项目而言（其实大部分项目都是如此）。我实在不需要那么多状态机的高级玩法：比如状态的嵌套（substate），状态的并行（parallel，fork，join）、子状态机等等。

开源状态机性能差：这些开源的状态机都是有状态的（Stateful）的，因为有状态，状态机的实例就不是线程安全的，而我们的应用服务器是分布式多线程的，所以在每一次状态机在接受请求的时候，都不得不重新build一个新的状态机实例。

所以COLA状态机设计的目标很明确，有两个核心理念：简洁的仅支持状态流转的状态机，不需要支持嵌套、并行等高级玩法。状态机本身需要是Stateless（无状态）的，这样一个Singleton
Instance就能服务所有的状态流转请求了。

COLA状态机的核心概念如下图所示，主要包括：

- State：状态

- Event：事件，状态由事件触发，引起变化

- Transition：流转，表示从一个状态到另一个状态

    - External Transition：外部流转，两个不同状态之间的流转
    - Internal Transition：内部流转，同一个状态之间的流转

- Condition：条件，表示是否允许到达某个状态

- Action：动作，到达某个状态之后，可以做什么

- StateMachine：状态机

  ![image.png](../images/statemachine.png)

### COLA状态机实战

首先配置状态机：

```java
/**
 * 状态机初始化
 */
@Configuration
public class LeaveStateMachineConfig {
    private static final Logger logger = LoggerFactory.getLogger(StateMachineRegist.class);
    private final String STATE_MACHINE_ID = "leaveStateMachineId";

    /**
     * 构建状态机实例
     */
    @Bean(name = STATE_MACHINE_ID)
    public StateMachine<ApplyStatusEnum, Event, LeaveContext> stateMachine() {
        StateMachineBuilder<ApplyStatusEnum, Event, LeaveContext> stateMachineBuilder =
            StateMachineBuilderFactory.create();
        // 员工请假触发事件
        // 因为没有源状态，初始化时只是同一个状态流转;所以用内部流转
        stateMachineBuilder.internalTransition().within(ApplyStatusEnum.LEAVE_SUBMIT).on(Event.EMPLOYEE_SUBMIT)
            .perform(doAction());

        // 部门主管审批触发事件（依赖上一个状态：LEAVE_SUBMIT）
        stateMachineBuilder.externalTransition().from(ApplyStatusEnum.LEAVE_SUBMIT).to(ApplyStatusEnum.LEADE_AUDIT_PASS)
            .on(Event.DIRECTLEADER_AUDIT).when(checkIfPass()).perform(doAction());
        stateMachineBuilder.externalTransition().from(ApplyStatusEnum.LEAVE_SUBMIT)
            .to(ApplyStatusEnum.LEADE_AUDIT_REFUSE).on(Event.DIRECTLEADER_AUDIT).when(checkIfNotPass())
            .perform(doAction());

        // hr事件触发(依赖上一个状态:LEADE_AUDIT_PASS)
        stateMachineBuilder.externalTransition().from(ApplyStatusEnum.LEADE_AUDIT_PASS).to(ApplyStatusEnum.HR_PASS)
            .on(Event.HR_AUDIT).when(checkIfPass()).perform(doAction());
        stateMachineBuilder.externalTransition().from(ApplyStatusEnum.LEADE_AUDIT_PASS).to(ApplyStatusEnum.HR_REFUSE)
            .on(Event.HR_AUDIT).when(checkIfNotPass()).perform(doAction());

        return stateMachineBuilder.build(STATE_MACHINE_ID);
    }

    private Condition<LeaveContext> checkIfPass() {
        return (ctx) -> ctx.getIdea().equals(0);
    }

    private Condition<LeaveContext> checkIfNotPass() {
        return (ctx) -> ctx.getIdea().equals(1);
    }

    /**
     * 事件触发后，匹配成功对应的条件后，就会执行具体动作<br>
     * 可以自定义完成具体业务逻辑...
     * @return
     */
    private Action<ApplyStatusEnum, Event, LeaveContext> doAction() {
        return (from, to, event, ctx) -> {
            logger.info("from:" + from + " to:" + to + " on:" + event + " condition:" + ctx);
        };
    }
}
```

然后在需要更改状态的地方，获取状态机实例，触发事件，获取更改后的状态：

```java
public class OrderService {
    @Autowired
    @Qualifier("leaveStateMachineId")
    StateMachine<OrderStatusEnum, OrderEvent, Order> orderOperaMachine;

    public void payOrder(Order order) {
        OrderStatus status = orderOperaMachine.fireEvent(order.status, Events.EVENT1, new Context());
        // 更新订单状态
    }
}
```

具体可以参考[cola-statemachine实践](https://lensman.com.cn/2023/03/14/hdqzkbp1ywg1gt1w/)

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>com.alibaba.cola</groupId>
        <artifactId>cola-component-statemachine</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- cola-component-statemachine：https://github.com/alibaba/COLA/blob/master/LICENSE