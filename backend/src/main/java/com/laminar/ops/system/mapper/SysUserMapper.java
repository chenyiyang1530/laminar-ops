package com.laminar.ops.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laminar.ops.system.entity.SysRole;
import com.laminar.ops.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("select r.* from sys_role r join sys_user_role ur on r.role_id = ur.role_id " +
            "where ur.user_id = #{userId} and r.status = '0'")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    @Select("select distinct m.perms from sys_menu m " +
            "join sys_role_menu rm on m.menu_id = rm.menu_id " +
            "join sys_user_role ur on ur.role_id = rm.role_id " +
            "where ur.user_id = #{userId} and m.status = '0' " +
            "and m.perms is not null and m.perms <> ''")
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
