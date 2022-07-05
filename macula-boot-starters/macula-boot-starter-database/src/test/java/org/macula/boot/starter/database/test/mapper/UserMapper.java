package org.macula.boot.starter.database.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.macula.boot.starter.database.test.entity.User;
import org.macula.boot.starter.database.test.vo.UserVO;

import java.util.List;

/**
 * <p>
 * <b>UserMapper</b> User实体的Mapper
 * </p>
 *
 * @author Rain
 * @since 2022-01-18
 */
public interface UserMapper extends BaseMapper<User> {

    List<UserVO> listById(@Param("id") Long id);

}
