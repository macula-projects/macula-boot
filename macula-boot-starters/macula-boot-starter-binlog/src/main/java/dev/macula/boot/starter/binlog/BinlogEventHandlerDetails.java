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

package dev.macula.boot.starter.binlog;

import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import dev.macula.boot.starter.binlog.utils.JDBCUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class BinlogEventHandlerDetails<T> {

    private static final ParserConfig snakeCase;

    static {
        snakeCase = new ParserConfig();
        snakeCase.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
    }

    private String database;
    private String table;
    private IBinlogEventHandler eventHandler;
    private BinlogClientConfig clientConfig;
    private Class<T> entityClass;

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
        return TypeUtils.cast(obj, entityClass, snakeCase);
    }
}
