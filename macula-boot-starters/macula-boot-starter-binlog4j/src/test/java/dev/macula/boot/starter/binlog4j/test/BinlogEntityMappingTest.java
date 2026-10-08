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
package dev.macula.boot.starter.binlog4j.test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;
import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import dev.macula.boot.starter.binlog4j.BinlogEventHandlerDetails;
import dev.macula.boot.starter.binlog4j.utils.JDBCUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.exc.InvalidFormatException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 Binlog 行数据经 Jackson 3 映射实体的兼容边界。
 *
 * @author Rain
 * @since 6.1.0
 */
class BinlogEntityMappingTest {

    @Test
    void mapsSnakeCaseAliasesAndDatabaseValueTypes() throws Exception {
        Entity entity = (Entity) convert(Entity.class,
                new String[]{"dept_id", "user_no", "amount", "enabled", "state", "payload", "created_at", "unknown"},
                new Serializable[]{42, "A001", new BigDecimal("12.30"), 1, "ACTIVE",
                        new byte[]{0, 1, -1}, new Date(1700000000000L), "ignored"}, 1000L);

        assertThat(entity.deptId).isEqualTo(42L);
        assertThat(entity.account).isEqualTo("A001");
        assertThat(entity.amount).isEqualByComparingTo("12.30");
        assertThat(entity.enabled).isTrue();
        assertThat(entity.state).isEqualTo(State.ACTIVE);
        assertThat(entity.payload).containsExactly((byte) 0, (byte) 1, (byte) -1);
        assertThat(entity.createdAt).isEqualTo(new Date(1700000001000L));
    }

    @Test
    void preservesNullValues() throws Exception {
        Entity entity = (Entity) convert(Entity.class,
                new String[]{"dept_id", "created_at", "enabled", "payload"},
                new Serializable[]{null, null, null, null}, 0L);
        assertThat(entity.deptId).isNull();
        assertThat(entity.createdAt).isNull();
        assertThat(entity.enabled).isNull();
        assertThat(entity.payload).isNull();
    }

    @Test
    void returnsOriginalColumnNamesWithoutEntityClass() throws Exception {
        Object result = convert(null, new String[]{"dept_id", "created_at"},
                new Serializable[]{42L, new Date(1000L)}, 500L);
        assertThat(result).isEqualTo(Map.of("dept_id", 42L, "created_at", new Date(1500L)));
    }

    @Test
    void rejectsInvalidNumericValue() {
        assertThatThrownBy(() -> convert(Entity.class, new String[]{"dept_id"},
                new Serializable[]{"not-a-number"}, 0L)).isInstanceOf(InvalidFormatException.class);
    }

    @SuppressWarnings("unchecked")
    private Object convert(Class<?> entityClass, String[] columns, Serializable[] values, long offset) throws Exception {
        BinlogClientConfig config = new BinlogClientConfig();
        config.setServerId(987654321L);
        config.setTimeOffset(offset);
        BinlogEventHandlerDetails details = new BinlogEventHandlerDetails();
        details.setClientConfig(config);
        details.setDatabase("test");
        details.setTable("sample");
        details.setEntityClass(entityClass);
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        int[] index = {-1};
        when(rows.next()).thenAnswer(invocation -> ++index[0] < columns.length);
        when(rows.getString(1)).thenAnswer(invocation -> columns[index[0]]);
        Map<Long, DataSource> sources = (Map<Long, DataSource>) ReflectionTestUtils.getField(JDBCUtils.class, "dataSourceMap");
        DataSource previous = sources.put(config.getServerId(), source);
        try {
            return details.toEntity(values);
        } finally {
            if (previous == null) {
                sources.remove(config.getServerId());
            } else {
                sources.put(config.getServerId(), previous);
            }
        }
    }

    /**
     * 包含常见数据库值类型的测试实体。
     *
     * @since 6.1.0
     */
    public static class Entity {
        public Long deptId;
        @JsonProperty("user_no")
        public String account;
        public BigDecimal amount;
        public Boolean enabled;
        public State state;
        public byte[] payload;
        public Date createdAt;
    }

    /**
     * 验证名称形式的枚举转换。
     *
     * @since 6.1.0
     */
    public enum State {
        ACTIVE
    }
}
