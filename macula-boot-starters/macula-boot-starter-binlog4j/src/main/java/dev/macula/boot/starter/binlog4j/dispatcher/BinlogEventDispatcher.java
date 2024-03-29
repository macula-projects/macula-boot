package dev.macula.boot.starter.binlog4j.dispatcher;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
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

public class BinlogEventDispatcher implements BinaryLogClient.EventListener {

    private final Map<Long, TableMapEventData> tableMap = new HashMap<>();

    private final List<BinlogEventHandlerDetails> eventHandlerMap;

    private final BinlogClientConfig clientConfig;

    private final BinlogPositionHandler binlogPositionHandler;

    public BinlogEventDispatcher(BinlogClientConfig clientConfig, BinlogPositionHandler positionHandler,
        List<BinlogEventHandlerDetails> eventHandlerMap) {
        this.clientConfig = clientConfig;
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
                            if (BinlogUtils.isUpdate(eventType))
                                eventHandler.invokeUpdate(rowMutationEventData.getUpdateRows());
                            if (BinlogUtils.isDelete(eventType))
                                eventHandler.invokeDelete(rowMutationEventData.getDeleteRows());
                            if (BinlogUtils.isInsert(eventType))
                                eventHandler.invokeInsert(rowMutationEventData.getInsertRows());
                        }
                    });
                }
            }
        }
        // 固化逻辑
        if (clientConfig.isPersistence()) {
            if (binlogPositionHandler != null) {
                BinlogPosition binlogPosition = new BinlogPosition();
                if (EventType.ROTATE == eventType) {
                    binlogPosition.setServerId(clientConfig.getServerId());
                    binlogPosition.setFilename(((RotateEventData)event.getData()).getBinlogFilename());
                    binlogPosition.setPosition(((RotateEventData)event.getData()).getBinlogPosition());
                } else {
                    binlogPosition = binlogPositionHandler.loadPosition(clientConfig.getServerId());
                    if (binlogPosition != null) {
                        binlogPosition.setPosition(headerV4.getNextPosition());
                    }
                }
                binlogPositionHandler.savePosition(binlogPosition);
            }
        }
    }

    @Data
    public static class RowMutationEventData {

        private long tableId;

        private List<Serializable[]> insertRows;

        private List<Serializable[]> deleteRows;

        private List<Map.Entry<Serializable[], Serializable[]>> updateRows;

        public RowMutationEventData(EventData eventData) {

            if (eventData instanceof UpdateRowsEventData) {
                UpdateRowsEventData updateRowsEventData = (UpdateRowsEventData)eventData;
                this.tableId = updateRowsEventData.getTableId();
                this.updateRows = updateRowsEventData.getRows();
            }

            if (eventData instanceof WriteRowsEventData) {
                WriteRowsEventData writeRowsEventData = (WriteRowsEventData)eventData;
                this.tableId = writeRowsEventData.getTableId();
                this.insertRows = writeRowsEventData.getRows();
            }

            if (eventData instanceof DeleteRowsEventData) {
                DeleteRowsEventData deleteRowsEventData = (DeleteRowsEventData)eventData;
                this.tableId = deleteRowsEventData.getTableId();
                this.deleteRows = deleteRowsEventData.getRows();
            }
        }
    }
}
