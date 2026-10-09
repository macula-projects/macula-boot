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

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import dev.macula.boot.starter.tinyid.base.generator.IdGenerator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证映射元数据解析与 TinyID 发号委托，不注册 Spring Bean。
 * @author Rain
 * @since 6.1.0
 */
class TinyIdIdentifierGeneratorTest {

    @Test
    void usesMappedTableAndPrimaryKeyColumn() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        assistant.setCurrentNamespace("test.orders");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        IdGeneratorFactory factory = mock(IdGeneratorFactory.class);
        IdGenerator ids = mock(IdGenerator.class);
        when(factory.getIdGenerator("sales_order_order_id")).thenReturn(ids);
        when(ids.nextId()).thenReturn(123L);
        assertThat(new TinyIdIdentifierGenerator(factory).nextId(new Order())).isEqualTo(123L);
        verify(factory).getIdGenerator("sales_order_order_id");
        RuntimeException failure = new IllegalStateException("unavailable");
        when(ids.nextId()).thenThrow(failure);
        assertThatThrownBy(() -> new TinyIdIdentifierGenerator(factory).nextId(new Order())).isSameAs(failure);
    }

    @Test
    void rejectsMissingInputOrMetadata() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TinyIdIdentifierGenerator(null));
        IdGeneratorFactory factory = mock(IdGeneratorFactory.class);
        TinyIdIdentifierGenerator generator = new TinyIdIdentifierGenerator(factory);
        assertThatIllegalArgumentException().isThrownBy(() -> generator.nextId(null));
        assertThatIllegalStateException().isThrownBy(() -> generator.nextId(new Object()));
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        assistant.setCurrentNamespace("test.noKey");
        TableInfoHelper.initTableInfo(assistant, NoKey.class);
        assertThatIllegalStateException().isThrownBy(() -> generator.nextId(new NoKey()));
        verifyNoInteractions(factory);
    }

    /**
     * 自定义数据库名称的测试实体。
     * @since 6.1.0
     */
    @TableName("sales_order")
    static class Order {
        @TableId("order_id")
        private Long id;
    }

    /**
     * 无主键映射的测试实体。
     * @since 6.1.0
     */
    @TableName("no_key")
    static class NoKey {
        private String name;
    }
}
