package dev.macula.boot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import dev.macula.boot.base.IBaseEnum;
import lombok.Getter;

public enum DataScopeEnum implements IBaseEnum<Integer> {
    ALL(0, "所有数据"), DEPT_AND_SUB(1, "部门及子部门数据"), DEPT(2, "本部门数据"), SELF(3, "本人数据"),
    DEFAULT(9, "默认范围");

    @Getter
    @EnumValue //  Mybatis-Plus 提供注解表示插入数据库时插入该值
    private Integer value;
    @Getter
    private String label;

    DataScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getLabel() {
        return this.label;
    }
}
