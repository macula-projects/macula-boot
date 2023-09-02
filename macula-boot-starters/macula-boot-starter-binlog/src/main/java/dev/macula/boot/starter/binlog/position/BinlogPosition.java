package dev.macula.boot.starter.binlog.position;

import lombok.ToString;

@ToString
public class BinlogPosition {

    private Long serverId;

    private Long position;

    private String filename;

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
