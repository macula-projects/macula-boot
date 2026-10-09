/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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
package dev.macula.boot.starter.tinyid.mybatisplus;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 使用映射表名与主键列名对应的 TinyID 业务生成主键。
 * 注册及 MyBatis-Plus 接入由使用方负责；服务端需预建相应业务。
 *
 * @author Rain
 * @since 6.1.0
 */
public class TinyIdIdentifierGenerator implements IdentifierGenerator {

    private final IdGeneratorFactory factory;

    /**
     * 创建生成器，不注册 Spring Bean。
     * @param factory 本地号段生成器工厂
     */
    public TinyIdIdentifierGenerator(IdGeneratorFactory factory) {
        Assert.notNull(factory, "IdGeneratorFactory must not be null");
        this.factory = factory;
    }

    /**
     * 使用“表名_主键列名”业务申请 ID；不处理动态物理分表名称。
     * @param entity MyBatis-Plus 已初始化映射的实体
     * @return 新主键
     * @throws IllegalArgumentException 实体为空
     * @throws IllegalStateException 表或主键元数据缺失
     */
    @Override
    public Long nextId(Object entity) {
        Assert.notNull(entity, "Entity must not be null");
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        if (tableInfo == null || !StringUtils.hasText(tableInfo.getTableName())
            || !StringUtils.hasText(tableInfo.getKeyColumn())) {
            throw new IllegalStateException("Missing MyBatis-Plus table or primary key metadata: "
                + entity.getClass().getName());
        }
        String bizType = tableInfo.getTableName() + "_" + tableInfo.getKeyColumn();
        return factory.getIdGenerator(bizType).nextId();
    }
}
