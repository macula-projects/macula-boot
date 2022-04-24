package org.macula.boot.starter.redis.test.vo;

import lombok.*;

/**
 * <p>
 * <b>User</b> 用户VO
 * </p>
 *
 * @author Rain
 * @since 2022-01-29
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class User implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String password;
}
