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

package dev.macula.boot.starter.binlog4j;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import dev.macula.boot.starter.binlog4j.utils.JDBCUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库表与 Binlog 事件处理器的映射信息。
 *
 * @author rain
 * @since 5.0.0
 */
@Data
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class BinlogEventHandlerDetails {

    private static final JsonMapper ENTITY_MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private String database;
    private String table;
    private IBinlogEventHandler eventHandler;
    private BinlogClientConfig clientConfig;
    private Class<?> entityClass;

    public void invokeInsert(List<Serializable[]> data) {
        data.forEach(row -> {
            eventHandler.onInsert(toEntity(row));
        });
    }

    public void invokeUpdate(List<Map.Entry<Serializable[], Serializable[]>> data) {
        data.forEach(row -> {
            eventHandler.onUpdate(toEntity(row.getKey()), toEntity(row.getValue()));
        });
    }

    public void invokeDelete(List<Serializable[]> data) {
        data.forEach(row -> {
            eventHandler.onDelete(toEntity(row));
        });
    }

    public Object toEntity(Serializable[] data) {
        String[] columnNames = JDBCUtils.getColumnNames(clientConfig, database, table);
        Map<String, Object> obj = new HashMap<>();
        for (int i = 0; i < data.length; i++) {
            Serializable field = data[i];
            if (field instanceof Date) {
                data[i] = new Date(((Date)field).getTime() + clientConfig.getTimeOffset());
            }
            obj.put(columnNames[i], data[i]);
        }
        if (entityClass == null) {
            return obj;
        }
        return ENTITY_MAPPER.convertValue(obj, entityClass);
    }
}
