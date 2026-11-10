package com.laminar.ops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laminar.ops.biz.entity.BizHospitalUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BizHospitalUserMapper extends BaseMapper<BizHospitalUser> {

    @Select("select * from biz_hospital_user where user_id = #{userId} limit 1")
    BizHospitalUser selectByUserId(@Param("userId") Long userId);
}
