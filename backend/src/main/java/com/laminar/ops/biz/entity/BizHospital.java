package com.laminar.ops.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_hospital")
public class BizHospital {

    @TableId(type = IdType.AUTO)
    private Long hospitalId;
    private String hospitalName;
    private String hospitalCode;
    private Long regionId;
    private String hospitalType;
    private String grade;
    private String nature;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String phone;
    private String contactPerson;
    private String status;
    private String remark;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String regionName;
}
