package com.laminar.ops.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 角色分配日志查询参数。 */
@Data
public class RoleAssignLogQuery {

    @Pattern(regexp = "^(assign|revoke|create|update|delete)?$", message = "不支持的动作类型")
    private String action = "";

    @Min(value = 1, message = "页码必须大于 0")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer pageSize = 10;
}
