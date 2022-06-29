package org.macula.boot.starter.mproot.test.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * <p>
 * <b>SexEnum</b> 性别枚举
 * </p>
 *
 * @author Rain
 * @since 2022-01-24
 */
public enum SexEnum {
    MALE("M", "男"),  FEMALE("F", "女");

    SexEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @EnumValue
    private final String code;
    private String desc;


    @Override
    public String toString() {
        return "SexEnum{" +
            "code='" + code + '\'' +
            ", desc='" + desc + '\'' +
            '}';
    }
}
