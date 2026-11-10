package com.laminar.ops.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserRoleMapper {

    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("insert into sys_user_role(user_id, role_id) values(#{userId}, #{roleId})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<Long> selectRoleIds(@Param("userId") Long userId);
}
