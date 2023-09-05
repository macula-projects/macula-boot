package dev.macula.boot.starter.binlog4j.config.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Documented
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BinlogSubscriber {

    /**
     * 客户端
     */
    String clientName();

    /**
     * 表名
     */
    String table();

    /**
     * 数据库
     */
    String database();

}
