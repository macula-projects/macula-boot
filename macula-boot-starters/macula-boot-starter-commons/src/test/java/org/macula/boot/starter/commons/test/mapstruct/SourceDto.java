package org.macula.boot.starter.commons.test.mapstruct;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class SourceDto {
    private String username;
    private String password;
    private Date currentTime;
    private int score;
}
