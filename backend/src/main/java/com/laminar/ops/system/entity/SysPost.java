package com.laminar.ops.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_post")
public class SysPost {

    @TableId(type = IdType.AUTO)
    private Long postId;
    private String postCode;
    private String postName;
    private Integer sort;
    private String status;
    private String remark;
}
