package org.macula.boot.starter.mproot.test.entity;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mybatis.mate.annotation.Algorithm;
import mybatis.mate.annotation.FieldEncrypt;
import org.macula.boot.starter.mproot.entity.BaseEntity;
import org.macula.boot.starter.mproot.test.entity.enums.SexEnum;

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
public class User extends BaseEntity {

    @FieldEncrypt(password = "xxx", algorithm = Algorithm.AES)
    private String name;

    private Integer age;

    private String email;

    private SexEnum sex;

    @Version
    private long version;
}
