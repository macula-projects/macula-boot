package dev.macula.boot.starter.binlog4j.position;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BinlogPosition {

    private Long serverId;

    private Long position;

    private String filename;

    private String gtidSet;
}
