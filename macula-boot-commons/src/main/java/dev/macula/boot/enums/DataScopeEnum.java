package dev.macula.boot.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import dev.macula.boot.base.IBaseEnum;
import lombok.Getter;

/**
 * 数据范围枚举
 *
 * @author rain
 * @since 2022/6/29 10:37
 */
public enum DataScopeEnum implements IBaseEnum<Integer> {
    /** 0 所有数据 */
    ALL(0, "所有数据"),
    /** 1 部门及子部门数据 */
    DEPT_AND_SUB(1, "部门及子部门数据"),
    /** 2 本部门数据 */
    DEPT(2, "本部门数据"),
    /** 3 本人数据 */
    SELF(3, "本人数据"),
    /** 9 默认范围 */
    DEFAULT(9, "默认范围");

    @Getter
    @EnumValue //  Mybatis-Plus 提供注解表示插入数据库时插入该值
    private final Integer value;

    @Getter
    private final String label;

    /**
     * 构造函数
     *
     * @param value 枚举值
     * @param label 枚举标签
     */
    DataScopeEnum(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
