package com.laminar.ops.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long deptId;
    private Long parentId;
    private String deptName;
    private String leader;
    private String phone;
    private String email;
    private Integer sort;
    private String status;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<SysDept> children;
}
