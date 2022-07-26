package dev.macula.example.consumer.vo;

import lombok.Data;

@Data
public class PoBaseResult {
    private boolean success;
    private String errCode;
    private String errDesc;
}
