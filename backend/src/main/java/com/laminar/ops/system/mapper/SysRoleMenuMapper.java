package com.laminar.ops.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMenuMapper {

    @Delete("delete from sys_role_menu where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("insert into sys_role_menu(role_id, menu_id) values(#{roleId}, #{menuId})")
    int insert(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Select("select menu_id from sys_role_menu where role_id = #{roleId}")
    List<Long> selectMenuIds(@Param("roleId") Long roleId);
}
