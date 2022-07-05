package org.macula.boot.starter.database.test.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.macula.boot.starter.database.entity.BaseEntity;
import org.macula.boot.starter.database.test.entity.enums.SexEnum;

/**
 * <p>
 * <b>User</b> 用户实体
 * </p>
 *
 * @author Rain
 * @since 2022-01-18
 */

@Getter
@Setter
@ToString
@TableName("T_USER")
public class User extends BaseEntity {

    private String name;

    private Integer age;

    private String email;

    private SexEnum sex;

    @Version
    private long version;
}
