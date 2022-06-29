package org.macula.boot.starter.mproot.test;

import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.incrementer.H2KeyGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.TemporalField;

/**
 * <p>
 * <b>MybatisPlusApplication</b> Application测试类
 * </p>
 *
 * @author Rain
 * @since 2022-01-18
 */
@SpringBootApplication
@MapperScan("org.macula.boot.starter.mproot.test.mapper")
public class MybatisPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisPlusApplication.class, args);
    }

    @Bean
    public IKeyGenerator keyGenerator() {
        return new H2KeyGenerator();
    }

//    @Bean
//    public IdentifierGenerator identifierGenerator() {
//        return new CustomIdGenerator();
//    }
//
//    public class  CustomIdGenerator implements IdentifierGenerator {
//        @Override
//        public Long nextId(Object entity) {
//            //可以将当前传入的class全类名来作为bizKey,或者提取参数来生成bizKey进行分布式Id调用生成.
//            String bizKey = entity.getClass().getName();
//            //根据bizKey调用分布式ID生成
//            long id = System.currentTimeMillis();
//            //返回生成的id值即可.
//            return id;
//        }
//    }
}