package org.macula.boot.starter.mproot.test.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.macula.boot.starter.mproot.entity.BaseEntity;

/**
 * <p>
 * <b>UserSeq</b> 使用序列作为ID
 * </p>
 *
 * @author Rain
 * @since 2022-01-23
 */
@Getter
@Setter
@TableName("user")
@KeySequence(value = "SEQ_USER")
public class UserSeq extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String name;

    private Integer age;

    private String email;

    @TableLogic
    private Integer deleted;

    @Version
    private long version;

    public UserSeq() {
    }

    public UserSeq(Long id) {
        this.id = id;
    }
}
