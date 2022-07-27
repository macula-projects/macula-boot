package dev.macula.example.consumer.vo;

import lombok.Data;

@Data
public class PoBaseResult<T> {
    private boolean success;
    private String errCode;
    private String errDesc;
    private T data;
}
