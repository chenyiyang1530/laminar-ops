package com.laminar.ops.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_hospital_user")
public class BizHospitalUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long hospitalId;
    private Long departmentId;
    private String roleType;
    private String status;
    private LocalDateTime createTime;
}
