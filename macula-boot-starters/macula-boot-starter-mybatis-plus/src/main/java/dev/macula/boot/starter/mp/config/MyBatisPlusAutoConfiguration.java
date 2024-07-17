/*
 * Copyright (c) 2023 Macula
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

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import dev.macula.boot.context.TenantContextHolder;
import dev.macula.boot.starter.crypto.config.CryptoProperties;
import dev.macula.boot.starter.crypto.core.CryptoManager;
import dev.macula.boot.starter.mp.handler.AuditMetaObjectHandler;
import dev.macula.boot.starter.mp.handler.MyDataPermissionHandler;
import dev.macula.boot.starter.mp.interceptor.MybatisDecryptInterceptor;
import dev.macula.boot.starter.mp.interceptor.MybatisEncryptInterceptor;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Objects;

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

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MyBatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 租户插件
        if (properties.isTenantEnable()) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override
                public Expression getTenantId() {

                    // 如果上下文租户ID不为空，则用上下文的，否则用配置的租户ID（适合租户负责人切换租户）
                    // TODO 需要检查当前用户是否可以访问该租户
                    if (Objects.nonNull(TenantContextHolder.getCurrentTenantId())) {
                        return new LongValue(TenantContextHolder.getCurrentTenantId());
                    }

                    // 获取当前登录用户的租户上下文（aksk访问带租户信息）
                    if (Objects.nonNull(SecurityUtils.getTenantId())) {
                        return new LongValue(SecurityUtils.getTenantId());
                    }

                    // 默认租户ID
                    return new LongValue(properties.getTenantId());
                }

                // 这是 default 方法,默认返回 false 表示所有表都需要拼多租户条件
                @Override
                public boolean ignoreTable(String tableName) {
                    // 分租户的表名称应该统一以xx标识
                    return Arrays.stream(properties.getTenantSuffixes()).noneMatch(tableName::endsWith);
                }
            }));
        }

        // 数据权限
        if (ClassUtils.isPresent("dev.macula.boot.starter.security.utils.SecurityUtils",
            this.getClass().getClassLoader())) {
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MyDataPermissionHandler()));
        }

        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setMaxLimit(properties.getMaxLimit());
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
    @ConditionalOnBean({CryptoManager.class, CryptoProperties.class})
    public MybatisEncryptInterceptor mybatisEncryptInterceptor(CryptoManager encryptorManager,
        CryptoProperties properties) {
        return new MybatisEncryptInterceptor(encryptorManager, properties);
    }

    @Bean
    @ConditionalOnBean({CryptoManager.class, CryptoProperties.class})
    public MybatisDecryptInterceptor mybatisDecryptInterceptor(CryptoManager encryptorManager,
        CryptoProperties properties) {
        return new MybatisDecryptInterceptor(encryptorManager, properties);
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler(MyBatisPlusProperties properties) {
        return new AuditMetaObjectHandler(properties.getAudit());
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return plusProperties -> {
            plusProperties.getGlobalConfig().setIdentifierGenerator(DefaultIdentifierGenerator.getInstance());
        };
    }
}
