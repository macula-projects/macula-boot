/*
 * Copyright (c) 2022 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.mp.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import dev.macula.boot.starter.mp.handler.AuditMetaObjectHandler;
import dev.macula.boot.starter.mp.interceptor.DecryptInterceptor;
import dev.macula.boot.starter.mp.interceptor.EncryptInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;

/**
 * <p>
 * <b>MyBatisPlusConfiguration</b> MyBatis Plus的默认配置
 * </p>
 *
 * @author Rain
 * @since 2022-01-21
 */
@AutoConfiguration
@EnableTransactionManagement
@EnableConfigurationProperties(MyBatisPlusProperties.class)
public class MyBatisPlusAutoConfiguration {
    /**
     * 分页时允许每页最大记录数
     */
    private static final Long MAX_LIMIT = 1000L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MyBatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(properties.getTenantId());
            }

            // 这是 default 方法,默认返回 false 表示所有表都需要拼多租户条件
            @Override
            public boolean ignoreTable(String tableName) {
                // 分租户的表名称应该统一以xx标识
                return !Arrays.stream(properties.getTenantSuffixes()).anyMatch(suffix -> tableName.endsWith(suffix));
            }
        }));

        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setMaxLimit(MAX_LIMIT);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        // 乐观锁插件，自动对具有@version的注解实体加上version条件
        OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor = new OptimisticLockerInnerInterceptor();
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor);

        // 防全表删除插件
        BlockAttackInnerInterceptor blockAttackInnerInterceptor = new BlockAttackInnerInterceptor();
        interceptor.addInnerInterceptor(blockAttackInnerInterceptor);
        return interceptor;
    }

    @Bean
    public EncryptInterceptor encryptInterceptor(MyBatisPlusProperties properties) {
        return new EncryptInterceptor(properties);
    }

    @Bean
    public DecryptInterceptor decryptInterceptor(MyBatisPlusProperties properties) {
        return new DecryptInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler(MyBatisPlusProperties properties) {
        return new AuditMetaObjectHandler(properties.getAudit());
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return plusProperties -> {
            // TODO 主键策略ASSIGN_ID对应的ID生成器
            plusProperties.getGlobalConfig().setIdentifierGenerator(new DefaultIdentifierGenerator());
        };
    }
}
