package org.macula.boot.starter.commons.test.mapstruct;

import lombok.Data;

import java.util.Date;

@Data
public class TargetDto {
    private String user;
    private String password;
    private Date currentTime;
    private int score;
}
