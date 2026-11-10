package com.laminar.ops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laminar.ops.biz.entity.BizEngineer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BizEngineerMapper extends BaseMapper<BizEngineer> {

    @Select("select * from biz_engineer where user_id = #{userId} limit 1")
    BizEngineer selectByUserId(@Param("userId") Long userId);
}
