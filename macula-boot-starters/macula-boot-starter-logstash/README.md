## 概述

本模块主要提供默认的日志格式配置、日志发送、日志审计等功能，由多个子模块组成。包括：

- macula-boot-starter-auditlog 日志审计记录
- macula-boot-starter-logstash 将日志发送给logstash

## 组件坐标

```xml
<!-- 发送日志到logstash -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-logstash</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用说明

### 日志默认配置

- springboot可以在应用配置文件中配置日志相关内容，例如：

```yaml
logging.level.root=info
logging.level.dev.macula.boot=debug
logging.file.name=${user.home}/logs/${spring.application.name}/${spring.application.name}.log
logging.pattern.console=>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n
logging.pattern.file=>%d{yyyy-MM-dd HH:mm:ss.SSS}==== [%thread] === %-5level %logger{50} - %msg%n
logging.config=xxx.xml  ## 如果以xml配置名称不是logback.xml或者logback-spring.xml，就需要在这里配置文件名称
```

- 使用logback.xml、logback-spring.xml或其他名称，配置了xml文件会失效应用配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
scan：当此属性设置为true时，配置文件如果发生改变，将会被重新加载，默认值为true。
scanPeriod：设置监测配置文件是否有修改的时间间隔，如果没有给出时间单位，默认单位是毫秒当scan为true时，此属性生效。默认的时间间隔为1分钟。
debug：当此属性设置为true时，将打印出logback内部日志信息，实时查看logback运行状态。默认值为false。
-->
<configuration scan="false" scanPeriod="60 seconds" debug="false">
    <!-- 定义日志的根目录 -->
    <property name="LOG_HOME" value="/app/log" />
    <!-- 定义日志文件名称 -->
    <property name="appName" value="springboot"></property>
    <!-- ch.qos.logback.core.ConsoleAppender 表示控制台输出 -->
    <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
        <!--
        日志输出格式：
			%d表示日期时间，
			%thread表示线程名，
			%-5level：级别从左显示5个字符宽度
			%logger{50} 表示logger名字最长50个字符，否则按照句点分割。
			%msg：日志消息，
			%n是换行符
        -->
        <springProfile name="prod">
            <!-- configuration to be enabled when the "production" profile is not active -->
            <layout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%d{yyyy-MM-dd}----- [%thread] ---- %-5level %logger{50} - %msg%n</pattern>
            </layout>
        </springProfile>
        <springProfile name="!prod">
            <!-- configuration to be enabled when the "production" profile is not active -->
            <layout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            </layout>
        </springProfile>
    </appender>

    <!-- 滚动记录文件，先将日志记录到指定文件，当符合某个条件时，将日志记录到其他文件 -->
    <appender name="appLogAppender" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- 指定日志文件的名称 -->
        <!--<file>${LOG_HOME}/${appName}.log</file>-->
        <file>F:/spring/spring.log</file>
        <!--
        当发生滚动时，决定 RollingFileAppender 的行为，涉及文件移动和重命名
        TimeBasedRollingPolicy： 最常用的滚动策略，它根据时间来制定滚动策略，既负责滚动也负责出发滚动。
        -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!--
            滚动时产生的文件的存放位置及文件名称 %d{yyyy-MM-dd}：按天进行日志滚动
            %i：当文件大小超过maxFileSize时，按照i进行文件滚动
            -->
            <fileNamePattern>${LOG_HOME}/${appName}-%d{yyyy-MM-dd}-%i.log</fileNamePattern>
            <!--
            可选节点，控制保留的归档文件的最大数量，超出数量就删除旧文件。假设设置每天滚动，
            且maxHistory是365，则只保存最近365天的文件，删除之前的旧文件。注意，删除旧文件是，
            那些为了归档而创建的目录也会被删除。
            -->
            <MaxHistory>365</MaxHistory>
            <!--
            当日志文件超过maxFileSize指定的大小是，根据上面提到的%i进行日志文件滚动 注意此处配置SizeBasedTriggeringPolicy是无法实现按文件大小进行滚动的，必须配置timeBasedFileNamingAndTriggeringPolicy
            -->
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <!-- 日志输出格式： -->
        <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [ %thread ] - [ %-5level ] [ %logger{50} : %line ] - %msg%n</pattern>
        </layout>
    </appender>

    <!--
		logger主要用于存放日志对象，也可以定义日志类型、级别
		name：表示匹配的logger类型前缀，也就是包的前半部分
		level：要记录的日志级别，包括 TRACE < DEBUG < INFO < WARN < ERROR
		additivity：作用在于children-logger是否使用 rootLogger配置的appender进行输出，
		false：表示只用当前logger的appender-ref，true：
		表示当前logger的appender-ref和rootLogger的appender-ref都有效
    -->
    <!-- hibernate logger -->
    <logger name="com.lcx" level="debug" />
    <!-- Spring framework logger -->
    <logger name="org.springframework" level="debug" additivity="false"></logger>



    <!--
    root与logger是父子关系，没有特别定义则默认为root，任何一个类只会和一个logger对应，
    要么是定义的logger，要么是root，判断的关键在于找到这个logger，然后判断这个logger的appender和level。
    -->
    <root level="info">
        <appender-ref ref="stdout" />
        <appender-ref ref="appLogAppender" />
    </root>
</configuration>
```

### Logstash

添加macula-boot-starter-logstash依赖，在你的logback的配置文件中，加入include，并且需要再application.yml中定义logstash.address配置logstash地址。

```xml
<include resource="logback-logstash.xml" />
```

上述inlcude的内容如下：

```xml
<included>
    <!-- logstash地址，从 application.yml 中获取-->
    <springProperty scope="context" name="LOGSTASH_ADDRESS" source="logstash.address"/>
    <springProperty scope="context" name="APPLICATION_NAME" source="spring.application.name"/>

    <!--输出到logstash的appender-->
    <appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <!--可以访问的logstash日志收集端口-->
        <destination>${LOGSTASH_ADDRESS}</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"spring.application.name":"${APPLICATION_NAME}"}</customFields>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="logstash"/>
    </root>
</included>
```

## 依赖引入

- logstash

  ```xml
  <dependencies>
      <dependency>
          <groupId>net.logstash.logback</groupId>
          <artifactId>logstash-logback-encoder</artifactId>
      </dependency>
  </dependencies>
  ```

## 版权说明

上述部分代码来源于[RuoYi](https://plus-doc.dromara.org)
并适当修改。基于MIT协议：https://gitee.com/dromara/RuoYi-Cloud-Plus/blob/master/LICENSE