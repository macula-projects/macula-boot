package dev.macula.boot.starter.binlog4j.position;

public interface BinlogPositionHandler {

    public BinlogPosition loadPosition(Long serverId);

    public void savePosition(BinlogPosition position);
}
