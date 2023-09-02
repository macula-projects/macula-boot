package dev.macula.boot.starter.binlog.position;

public interface BinlogPositionHandler {

    public BinlogPosition loadPosition(Long serverId);

    public void savePosition(BinlogPosition position);
}
