## 概述

定时任务接入模块，支持下列定义的定时任务：

- xxljob注解
- Spring Scheduler注解

## 客户端介绍

### 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-task-xxljob</artifactId>
</dependency>
```

### 使用配置

#### 接入xxljob的配置如下：

```yaml
xxl:
  job:
    accessToken: default_token
    admin:
      #  addresses: http://127.0.0.1:8080/xxl-job-admin  # 调度中心部署跟地址 [选填]：如调度中心集群部署存在多个地址则用逗号分隔。 执行器将会使用该地址进行"执行器心跳注册"和"任务结果回调"；为空则关闭自动注册；
      name: macula-cloud-xxljob                          # 注册到注册中心的Admin的服务名称[选填]，默认是xxl-job-admin，和addresses二选一
    executor:
      appname: ${spring.application.name}                # 执行器AppName [选填]：执行器心跳注册分组依据；为空则关闭自动注册
      #  address:                                        # 服务注册地址,优先使用该配置作为注册地址 为空时使用内嵌服务 ”IP:PORT“ 作为注册地址 从而更灵活的支持容器类型执行器动态IP和动态映射端口问题
      #  ip:                                             # 执行器IP [选填]：默认为空表示自动获取IP，多网卡时可手动设置指定IP ，该IP不会绑定Host仅作为通讯实用；地址信息用于 "执行器注册" 和 "调度中心请求并触发任务"
      #  port: 9999                                      # 执行器端口号 [选填]：小于等于0则自动获取；默认端口为9099，单机部署多个执行器时，注意要配置不同执行器端口；
      logpath: ${user.home}/logs/${spring.application.name}    # 执行器运行日志文件存储磁盘路径 [选填] ：需要对该路径拥有读写权限；为空则使用默认路径；
      logretentiondays: 30                             # 执行器日志保存天数 [选填] ：值大于3时生效，启用执行器Log文件定期清理功能，否则不生效；
```

### 核心功能

#### 使用@XxlJob注解定义任务

```java
/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤： 1、任务开发：在Spring Bean实例中，开发Job方法； 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法",
 * destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
public class XxlJobDemoHandler {
    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        XxlJobHelper.log("XXL-JOB, Hello World.");

        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        // default success
    }

    /**
     * 3、命令行任务
     */
    @XxlJob("commandJobHandler")
    public void commandJobHandler() throws Exception {
        String command = XxlJobHelper.getJobParam();
        int exitValue = -1;

        BufferedReader bufferedReader = null;
        try {
            // command process
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            // Process process = Runtime.getRuntime().exec(command);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

            // command log
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                XxlJobHelper.log(line);
            }

            // command exit
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            XxlJobHelper.log(e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        if (exitValue == 0) {
            // default success
        } else {
            XxlJobHelper.handleFail("command exit value(" + exitValue + ") is failed");
        }
    }
}
```

### 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <!--xxl job-->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
    </dependency>
    <!--提供服务发现能力-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-commons</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 服务端介绍

#### xxl-job

请参考xxljob[官网](https://www.xuxueli.com/xxl-job/)。由于服务端需要回调客户端，建议各平台拷贝macula-cloud-xxjob部署到自己的集群中。

## 版权说明

- Xxljob：https://github.com/xuxueli/xxl-job/blob/master/LICENSE