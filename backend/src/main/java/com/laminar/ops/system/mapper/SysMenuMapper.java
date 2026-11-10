package com.laminar.ops.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laminar.ops.system.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("select distinct m.* from sys_menu m " +
            "join sys_role_menu rm on m.menu_id = rm.menu_id " +
            "join sys_user_role ur on ur.role_id = rm.role_id " +
            "where ur.user_id = #{userId} and m.menu_type in ('M','C') " +
            "and m.visible = '0' and m.status = '0' " +
            "order by m.parent_id, m.order_num")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    @Select("select * from sys_menu where menu_type in ('M','C') and visible = '0' and status = '0' " +
            "order by parent_id, order_num")
    List<SysMenu> selectRouterMenus();

    @Select("select * from sys_menu order by parent_id, order_num")
    List<SysMenu> selectAllMenus();
}
