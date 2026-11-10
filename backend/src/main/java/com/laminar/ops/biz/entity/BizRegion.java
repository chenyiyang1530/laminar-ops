package com.laminar.ops.biz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_region")
public class BizRegion {

    @TableId(type = IdType.AUTO)
    private Long regionId;
    private Long companyId;
    private String regionName;
    private String regionCode;
    private String manager;
    private String phone;
    private String status;
    private String remark;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
