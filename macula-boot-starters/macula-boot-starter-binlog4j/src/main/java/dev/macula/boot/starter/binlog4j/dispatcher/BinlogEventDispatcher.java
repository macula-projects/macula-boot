/*
 * Copyright (c) 2023-2026 Macula
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

package dev.macula.boot.starter.binlog4j.dispatcher;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.AnnotateRowsEventData;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import dev.macula.boot.starter.binlog4j.BinlogEventHandlerDetails;
import dev.macula.boot.starter.binlog4j.BinlogUtils;
import dev.macula.boot.starter.binlog4j.position.BinlogPosition;
import dev.macula.boot.starter.binlog4j.position.BinlogPositionHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将原始 Binlog 事件转换并路由至表级处理器。
 *
 * @author rain
 * @since 5.0.0
 */
public class BinlogEventDispatcher implements BinaryLogClient.EventListener {

    private final Map<Long, TableMapEventData> tableMap = new HashMap<>();

    private final List<BinlogEventHandlerDetails> eventHandlerMap;

    private final BinlogClientConfig clientConfig;
    private final BinaryLogClient client;
    private final BinlogPositionHandler binlogPositionHandler;

    private boolean tx;

    public BinlogEventDispatcher(BinlogClientConfig clientConfig, BinaryLogClient client, BinlogPositionHandler positionHandler,
                                 List<BinlogEventHandlerDetails> eventHandlerMap) {
        this.clientConfig = clientConfig;
        this.client = client;
        this.eventHandlerMap = eventHandlerMap;
        this.binlogPositionHandler = positionHandler;
    }

    @Override
    public void onEvent(Event event) {
        EventHeaderV4 headerV4 = event.getHeader();
        EventType eventType = headerV4.getEventType();
        if (eventType == EventType.TABLE_MAP) {
            TableMapEventData eventData = event.getData();
            String database = eventData.getDatabase();
            String table = eventData.getTable();
            eventHandlerMap.forEach(eventHandler -> {
                if (eventHandler.getDatabase().equals(database) && eventHandler.getTable().equals(table)) {
                    tableMap.put(eventData.getTableId(), eventData);
                }
            });
        } else {
            if (EventType.isRowMutation(eventType)) {
                RowMutationEventData rowMutationEventData = new RowMutationEventData(event.getData());
                TableMapEventData tableMapEventData = tableMap.get(rowMutationEventData.getTableId());
                if (tableMapEventData != null) {
                    String database = tableMapEventData.getDatabase();
                    String table = tableMapEventData.getTable();
                    this.eventHandlerMap.forEach((eventHandler) -> {
                        if (eventHandler.getDatabase().equals(database) && eventHandler.getTable().equals(table)) {
                            if (BinlogUtils.isUpdate(eventType)) {
                                eventHandler.invokeUpdate(rowMutationEventData.getUpdateRows());
                            }
                            if (BinlogUtils.isDelete(eventType)) {
                                eventHandler.invokeDelete(rowMutationEventData.getDeleteRows());
                            }
                            if (BinlogUtils.isInsert(eventType)) {
                                eventHandler.invokeInsert(rowMutationEventData.getInsertRows());
                            }
                        }
                    });
                }
            }
        }
        // 固化逻辑
        if (clientConfig.isPersistence() && binlogPositionHandler != null) {
            BinlogPosition binlogPosition = new BinlogPosition();
            if (EventType.ROTATE == eventType) {
                binlogPosition.setServerId(clientConfig.getServerId());
                binlogPosition.setFilename(((RotateEventData) event.getData()).getBinlogFilename());
                binlogPosition.setPosition(((RotateEventData) event.getData()).getBinlogPosition());
                binlogPosition.setGtidSet(client.getGtidSet());
            } else {
                binlogPosition = binlogPositionHandler.loadPosition(clientConfig.getServerId());
                if (binlogPosition != null) {
                    binlogPosition.setPosition(headerV4.getNextPosition());

                    // 缓存GTID
                    switch (eventType) {
                        case XID:
                            commitGtid("COMMIT", binlogPosition);
                            break;
                        case QUERY:
                            QueryEventData queryEventData = (QueryEventData) EventDeserializer.EventDataWrapper.internal(event.getData());
                            String sql = queryEventData.getSql();
                            if (sql == null) {
                                break;
                            }
                            commitGtid(sql, binlogPosition);
                            break;
                        case ANNOTATE_ROWS:
                            AnnotateRowsEventData annotateRowsEventData = (AnnotateRowsEventData) EventDeserializer.EventDataWrapper.internal(event.getData());
                            sql = annotateRowsEventData.getRowsQuery();
                            if (sql == null) {
                                break;
                            }
                            commitGtid(sql, binlogPosition);
                            break;
                    }
                }
            }
            binlogPositionHandler.savePosition(binlogPosition);
        }
    }

    private void commitGtid(String sql, BinlogPosition binlogPosition) {
        if ("BEGIN".equals(sql)) {
            tx = true;
        } else if ("COMMIT".equals(sql) || "ROLLBACK".equals(sql)) {
            binlogPosition.setGtidSet(client.getGtidSet());
            tx = false;
        } else if (!tx) {
            // auto-commit query, likely DDL
            binlogPosition.setGtidSet(client.getGtidSet());
        }
    }

    /**
     * 统一表示行插入、删除和更新数据的事件模型。
     *
     * @since 5.0.0
     */
    @Data
    public static class RowMutationEventData {

        private long tableId;

        private List<Serializable[]> insertRows;

        private List<Serializable[]> deleteRows;

        private List<Map.Entry<Serializable[], Serializable[]>> updateRows;

        public RowMutationEventData(EventData eventData) {

            if (eventData instanceof UpdateRowsEventData) {
                UpdateRowsEventData updateRowsEventData = (UpdateRowsEventData) eventData;
                this.tableId = updateRowsEventData.getTableId();
                this.updateRows = updateRowsEventData.getRows();
            }

            if (eventData instanceof WriteRowsEventData) {
                WriteRowsEventData writeRowsEventData = (WriteRowsEventData) eventData;
                this.tableId = writeRowsEventData.getTableId();
                this.insertRows = writeRowsEventData.getRows();
            }

            if (eventData instanceof DeleteRowsEventData) {
                DeleteRowsEventData deleteRowsEventData = (DeleteRowsEventData) eventData;
                this.tableId = deleteRowsEventData.getTableId();
                this.deleteRows = deleteRowsEventData.getRows();
            }
        }
    }
}
