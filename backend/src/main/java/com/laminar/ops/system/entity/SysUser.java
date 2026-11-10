package com.laminar.ops.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long userId;
    private String username;
    @JsonIgnore
    private String password;
    private Integer mustChangePwd;
    private Integer loginFailCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockUntil;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pwdUpdateTime;
    private String nickName;
    private String avatar;
    private String phone;
    private String email;
    private Integer phoneVerified;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime phoneVerifiedAt;
    private Integer emailVerified;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime emailVerifiedAt;
    private String gender;
    private Long deptId;
    private Long postId;
    private Long companyId;
    private Integer prefPageSize;
    private String userType;
    private String status;
    private String loginIp;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginDate;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private String remark;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String postName;
    @TableField(exist = false)
    private List<Long> roleIds;
    @TableField(exist = false)
    private List<String> roleNames;
    @TableField(exist = false)
    private String hospitalName;
}
