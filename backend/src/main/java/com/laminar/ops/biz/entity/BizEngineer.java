package com.laminar.ops.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_engineer")
public class BizEngineer {

    @TableId(type = IdType.AUTO)
    private Long engineerId;
    private Long userId;
    private Long companyId;
    private Long regionId;
    private String engineerName;
    private String phone;
    private String gender;
    private String jobTitle;
    private String specialty;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;
    private String onDuty;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status;
    private String remark;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
