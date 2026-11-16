package com.laminar.ops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laminar.ops.biz.entity.BizHospital;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BizHospitalMapper extends BaseMapper<BizHospital> {

    @Select("<script>" +
            "select h.*, r.region_name, " +
            "(select group_concat(c2.company_name separator '、') from biz_company_hospital ch " +
            "join biz_company c2 on c2.company_id=ch.company_id where ch.hospital_id=h.hospital_id) as service_company_names " +
            "from biz_hospital h " +
            "left join biz_region r on r.region_id = h.region_id " +
            "<where>" +
            "<if test='q.hospitalName != null and q.hospitalName != \"\"'> and h.hospital_name like concat('%', #{q.hospitalName}, '%')</if>" +
            "<if test='q.hospitalCode != null and q.hospitalCode != \"\"'> and h.hospital_code like concat('%', #{q.hospitalCode}, '%')</if>" +
            "<if test='q.grade != null and q.grade != \"\"'> and h.grade = #{q.grade}</if>" +
            "<if test='q.status != null and q.status != \"\"'> and h.status = #{q.status}</if>" +
            "<if test='q.regionId != null'> and h.region_id = #{q.regionId}</if>" +
            "<if test='hospitalIds != null and hospitalIds.size() > 0'> and h.hospital_id in " +
            "<foreach collection='hospitalIds' item='hid' open='(' separator=',' close=')'>#{hid}</foreach></if>" +
            "<if test='hospitalIds != null and hospitalIds.size() == 0'> and 1=0</if>" +
            "</where>" +
            " order by h.hospital_id desc" +
            "</script>")
    IPage<BizHospital> selectPageWithRegion(IPage<BizHospital> page, @Param("q") BizHospital q,
                                            @Param("hospitalIds") List<Long> hospitalIds);

    @Select("select h.*, r.region_name, " +
            "(select group_concat(c2.company_name separator '、') from biz_company_hospital ch " +
            "join biz_company c2 on c2.company_id=ch.company_id where ch.hospital_id=h.hospital_id) as service_company_names " +
            "from biz_hospital h " +
            "left join biz_region r on r.region_id = h.region_id where h.hospital_id = #{id}")
    BizHospital selectDetail(@Param("id") Long id);
}
