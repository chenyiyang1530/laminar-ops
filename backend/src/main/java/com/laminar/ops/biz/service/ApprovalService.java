package com.laminar.ops.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final JdbcTemplate jdbc;
    private final LeaveService leaveService;
    private final DataScopeService dataScope;
    private final ObjectMapper objectMapper;

    @Value("${laminar.approval.timeout-hours:24}")
    private int timeoutHours;

    @Value("${laminar.approval.delegate-remind-days:3}")
    private int delegateRemindDays;

    public List<Map<String, Object>> pending() {
        Long userId = AuthContext.require().getUserId();
        return jdbc.queryForList(
                "select t.task_id as `taskId`, t.biz_type as `bizType`, t.biz_id as `bizId`, t.step_no as `stepNo`, " +
                        "t.create_time as `createTime`, l.leave_id as `leaveId`, e.engineer_name as `engineerName`, " +
                        "l.leave_type as `leaveType`, l.start_date as `startDate`, l.end_date as `endDate`, l.days, l.reason " +
                        "from biz_approval_task t join biz_engineer_leave l on l.leave_id=t.biz_id " +
                        "join biz_engineer e on e.engineer_id=l.engineer_id " +
                        "where t.biz_type='engineer_leave' and t.approver_id=? and t.status='PENDING' " +
                        "order by t.create_time", userId);
    }

    public List<Map<String, Object>> companyRecords() {
        LoginUser user = AuthContext.require();
        boolean allowed = user.isSuperAdmin() || user.getRoleKeys().contains("company_executive")
                || user.getRoleKeys().contains("maintenance_manager");
        if (!allowed) throw BizException.forbidden("只有公司高管可以查看全公司审批记录");
        Long companyId = dataScope.companyId(user);
        String sql = "select l.leave_id as `leaveId`, e.engineer_name as `engineerName`, l.leave_type as `leaveType`, " +
                "l.start_date as `startDate`, l.end_date as `endDate`, l.days, l.reason, l.status, " +
                "l.current_step as `currentStep`, l.create_time as `createTime`, " +
                "(select t.task_id from biz_approval_task t where t.biz_type='engineer_leave' and t.biz_id=l.leave_id " +
                "and t.status='PENDING' order by t.step_no limit 1) as `currentTaskId`, " +
                "(select u.nick_name from biz_approval_task t left join sys_user u on u.user_id=t.approver_id " +
                "where t.biz_type='engineer_leave' and t.biz_id=l.leave_id and t.status='PENDING' " +
                "order by t.step_no limit 1) as `currentApproverName`, " +
                "(select group_concat(concat(t.step_no, '级 ', coalesce(u.nick_name, '—'), ' ', " +
                "case t.status when 'APPROVED' then '通过' when 'REJECTED' then '驳回' " +
                "when 'CANCELLED' then '取消' else '待审批' end) order by t.step_no separator '；') " +
                "from biz_approval_task t left join sys_user u on u.user_id=t.approver_id " +
                "where t.biz_type='engineer_leave' and t.biz_id=l.leave_id) as `approvalTrail` " +
                "from biz_engineer_leave l join biz_engineer e on e.engineer_id=l.engineer_id " +
                (companyId == null ? " order by l.leave_id desc limit 200" : " where l.company_id=? order by l.leave_id desc limit 200");
        return companyId == null ? jdbc.queryForList(sql) : jdbc.queryForList(sql, companyId);
    }

    public List<Map<String, Object>> history() {
        Long userId = AuthContext.require().getUserId();
        return jdbc.queryForList(
                "select t.task_id as `taskId`, t.biz_type as `bizType`, t.biz_id as `bizId`, t.step_no as `stepNo`, " +
                        "t.status, t.action, t.comment, t.acted_at as `actedAt`, e.engineer_name as `engineerName`, " +
                        "l.leave_id as `leaveId`, l.leave_type as `leaveType`, l.start_date as `startDate`, l.end_date as `endDate`, l.days " +
                        "from biz_approval_task t join biz_engineer_leave l on l.leave_id=t.biz_id " +
                        "join biz_engineer e on e.engineer_id=l.engineer_id " +
                        "where t.biz_type='engineer_leave' and t.approver_id=? and t.status in ('APPROVED','REJECTED') " +
                        "order by t.acted_at desc limit 100", userId);
    }

    public void act(Long taskId, boolean approved, String comment) {
        Long userId = AuthContext.require().getUserId();
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "select biz_type as `bizType`, biz_id as `bizId`, approver_id as `approverId`, status " +
                        "from biz_approval_task where task_id=?", taskId);
        if (tasks.isEmpty()) throw BizException.badRequest("审批任务不存在");
        Map<String, Object> task = tasks.get(0);
        if (!userId.equals(((Number) task.get("approverId")).longValue())) {
            throw BizException.forbidden("无权处理该审批任务");
        }
        if (!"PENDING".equals(task.get("status"))) throw BizException.badRequest("该审批任务已处理");
        if (!"engineer_leave".equals(task.get("bizType"))) {
            throw BizException.badRequest("暂不支持该审批类型");
        }
        leaveService.approve(((Number) task.get("bizId")).longValue(), comment, approved);
        logAction(taskId, "engineer_leave", ((Number) task.get("bizId")).longValue(),
                approved ? "APPROVE" : "REJECT", userId, null, comment);
    }

    public List<Map<String, Object>> transferOptions() {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        int actorRank = userRank(user.getUserId());
        if (companyId == null) {
            if (user.isSuperAdmin()) {
                return jdbc.queryForList(
                        "select u.user_id as `value`, concat(u.nick_name,'（',coalesce(d.dept_name,'未分配部门'),'）') as `label` " +
                                "from sys_user u left join sys_dept d on d.dept_id=u.dept_id " +
                                "where u.status='0' and u.user_type<>'2' and u.user_id<>? order by u.nick_name",
                        user.getUserId());
            }
        }
        String companyFilter = companyId == null ? "" : " and u.company_id=? ";
        List<Object> args = new ArrayList<>();
        args.add(user.getUserId());
        if (companyId != null) args.add(companyId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select u.user_id as `value`, concat(u.nick_name,'（',coalesce(d.dept_name,'未分配部门'),'）') as `label`, " +
                        "coalesce(max(case r.role_key " +
                        "when 'super_admin' then 1000 when 'company_admin' then 900 when 'company_sub_admin' then 800 " +
                        "when 'company_executive' then 700 when 'maintenance_manager' then 600 " +
                        "when 'regional_manager' then 500 when 'engineer' then 400 when 'department_staff' then 300 " +
                        "else 10 end), 0) as `rank` " +
                        "from sys_user u left join sys_user_role ur on ur.user_id=u.user_id " +
                        "left join sys_role r on r.role_id=ur.role_id left join sys_dept d on d.dept_id=u.dept_id " +
                        "where u.status='0' and u.user_type<>'2' and u.user_id<>?" + companyFilter +
                        " group by u.user_id, u.nick_name, d.dept_name order by u.nick_name",
                args.toArray());
        return rows.stream().filter(r -> ((Number) r.get("rank")).intValue() >= actorRank)
                .peek(r -> r.remove("rank")).toList();
    }

    public void transfer(Long taskId, Long targetUserId) {
        LoginUser user = AuthContext.require();
        if (targetUserId == null || targetUserId.equals(user.getUserId())) {
            throw BizException.badRequest("请选择其他审批人");
        }
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "select t.approver_id as `approverId`, t.status, t.biz_id as `bizId`, l.company_id as `companyId` " +
                        "from biz_approval_task t left join biz_engineer_leave l on l.leave_id=t.biz_id where t.task_id=?",
                taskId);
        if (tasks.isEmpty()) throw BizException.badRequest("审批任务不存在");
        Map<String, Object> task = tasks.get(0);
        Long ownerCompany = task.get("companyId") == null ? null : ((Number) task.get("companyId")).longValue();
        Long currentCompany = dataScope.companyId(user);
        boolean approver = user.getUserId().equals(((Number) task.get("approverId")).longValue());
        boolean manager = user.getRoleKeys().contains("company_executive") || user.getRoleKeys().contains("maintenance_manager");
        if (!user.isSuperAdmin() && !approver && !(manager && currentCompany != null && currentCompany.equals(ownerCompany))) {
            throw BizException.forbidden("无权转交该审批任务");
        }
        if (!"PENDING".equals(task.get("status"))) throw BizException.badRequest("该审批任务已处理");
        List<Map<String, Object>> target = ownerCompany == null
                ? jdbc.queryForList("select user_id, status, user_type from sys_user where user_id=?", targetUserId)
                : jdbc.queryForList("select user_id, status, user_type, company_id from sys_user where user_id=? and company_id=?",
                targetUserId, ownerCompany);
        if (target.isEmpty() || !"0".equals(target.get(0).get("status"))) {
            throw BizException.badRequest("目标审批人不可用");
        }
        if (!user.isSuperAdmin() && userRank(targetUserId) < userRank(user.getUserId())) {
            throw BizException.badRequest("只能转交给上级或同级审批人");
        }
        jdbc.update("update biz_approval_task set approver_id=? where task_id=?", targetUserId, taskId);
        logAction(taskId, "engineer_leave", ((Number) task.get("bizId")).longValue(),
                "TRANSFER", user.getUserId(), targetUserId, null);
        jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,receiver_id,company_id,is_read,create_time) " +
                        "values('approval',?,?,'2','approval_task',?,?,?,'0',now())",
                "审批转交", user.getNickName() + " 将一条请假审批转交给你处理", taskId, targetUserId, ownerCompany);
    }

    private int userRank(Long userId) {
        List<String> keys = jdbc.queryForList(
                "select r.role_key from sys_user_role ur join sys_role r on r.role_id=ur.role_id where ur.user_id=?",
                String.class, userId);
        int rank = 0;
        for (String key : keys) rank = Math.max(rank, roleRank(key));
        return rank;
    }

    private int roleRank(String roleKey) {
        return switch (roleKey == null ? "" : roleKey) {
            case "super_admin" -> 1000;
            case "company_admin" -> 900;
            case "company_sub_admin" -> 800;
            case "company_executive" -> 700;
            case "maintenance_manager" -> 600;
            case "regional_manager" -> 500;
            case "engineer" -> 400;
            case "department_staff" -> 300;
            case "hospital_admin" -> 200;
            case "hospital_sub_admin" -> 150;
            case "hospital_dept" -> 100;
            default -> 10;
        };
    }

    public void urge(Long taskId) {
        LoginUser user = AuthContext.require();
        boolean manager = user.getRoleKeys().contains("company_executive") || user.getRoleKeys().contains("maintenance_manager");
        if (!manager && !user.isSuperAdmin()) throw BizException.forbidden("只有公司高管可以督办审批");
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "select t.approver_id as `approverId`, t.status, t.biz_id as `bizId`, l.company_id as `companyId`, " +
                        "e.engineer_name as `engineerName` from biz_approval_task t " +
                        "left join biz_engineer_leave l on l.leave_id=t.biz_id " +
                        "left join biz_engineer e on e.engineer_id=l.engineer_id where t.task_id=?", taskId);
        if (tasks.isEmpty()) throw BizException.badRequest("审批任务不存在");
        Map<String, Object> task = tasks.get(0);
        if (!"PENDING".equals(task.get("status"))) throw BizException.badRequest("该审批任务已处理");
        Long ownerCompany = task.get("companyId") == null ? null : ((Number) task.get("companyId")).longValue();
        Long currentCompany = dataScope.companyId(user);
        if (!user.isSuperAdmin() && (currentCompany == null || !currentCompany.equals(ownerCompany))) {
            throw BizException.forbidden("无权督办其他公司的审批");
        }
        Long approver = ((Number) task.get("approverId")).longValue();
        jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                        "receiver_role,company_id,is_read,create_time) values('approval',?,?,'2','approval_task',?,?,?,null,?,'0',now())",
                "审批督办", user.getNickName() + " 督办了 " + task.get("engineerName") + " 的请假审批，请尽快处理",
                taskId, "approval:urge:" + taskId + ":" + LocalDate.now() + ":" + approver, approver, ownerCompany);
    }

    public Map<String, Object> flow(Long leaveId) {
        LoginUser user = AuthContext.require();
        Map<String, Object> leave = jdbc.queryForMap(
                "select l.leave_id as `leaveId`, l.engineer_id as `engineerId`, l.company_id as `companyId`, " +
                        "l.leave_type as `leaveType`, l.start_date as `startDate`, l.end_date as `endDate`, l.days, " +
                        "l.reason, l.status, e.engineer_name as `engineerName` " +
                        "from biz_engineer_leave l join biz_engineer e on e.engineer_id=l.engineer_id where l.leave_id=?",
                leaveId);
        Long companyId = dataScope.companyId(user);
        Object targetCompany = leave.get("companyId");
        if (companyId != null && (targetCompany == null
                || !companyId.equals(((Number) targetCompany).longValue()))) {
            throw BizException.forbidden("无权查看其他公司请假");
        }
        if ("self".equals(user.getDataScope())
                && (user.getEngineerId() == null
                || !user.getEngineerId().equals(((Number) leave.get("engineerId")).longValue()))) {
            throw BizException.forbidden("无权查看其他工程师请假");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leave", leave);
        result.put("tasks", jdbc.queryForList(
                "select t.step_no as `stepNo`, t.status, t.comment, t.create_time as `createTime`, t.acted_at as `actedAt`, " +
                        "u.nick_name as `approverName` from biz_approval_task t " +
                        "left join sys_user u on u.user_id=t.approver_id " +
                        "where t.biz_type='engineer_leave' and t.biz_id=? order by t.step_no, t.task_id",
                leaveId));
        result.put("logs", jdbc.queryForList(
                "select l.action, l.comment, l.create_time as `createTime`, " +
                        "f.nick_name as `fromName`, tgt.nick_name as `toName` " +
                        "from biz_approval_action_log l " +
                        "left join sys_user f on f.user_id=l.operator_id " +
                        "left join sys_user tgt on tgt.user_id=l.target_user_id " +
                        "where l.biz_type='engineer_leave' and l.biz_id=? order by l.log_id",
                leaveId));
        return result;
    }

    private void logAction(Long taskId, String bizType, Long bizId, String action,
                           Long operatorId, Long targetUserId, String comment) {
        jdbc.update("insert into biz_approval_action_log(task_id,biz_type,biz_id,action,operator_id,target_user_id,comment,create_time) " +
                        "values(?,?,?,?,?,?,?,now())",
                taskId, bizType, bizId, action, operatorId, targetUserId, comment);
    }

    public List<Map<String, Object>> delegates() {
        Long userId = AuthContext.require().getUserId();
        return jdbc.queryForList(
                "select d.delegate_id as `delegateId`, d.user_id as `userId`, d.target_user_id as `targetUserId`, " +
                        "u.nick_name as `targetName`, d.start_date as `startDate`, d.end_date as `endDate`, " +
                        "case when d.status='1' then '1' when d.end_date < curdate() then '2' else '0' end as `status` " +
                        "from biz_approval_delegate d left join sys_user u on u.user_id=d.target_user_id " +
                        "where d.user_id=? order by d.start_date desc", userId);
    }

    public void saveDelegate(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Long target = body.get("targetUserId") == null ? null : Long.valueOf(String.valueOf(body.get("targetUserId")));
        if (target == null || target.equals(user.getUserId())) throw BizException.badRequest("请选择其他代理审批人");
        LocalDate start = LocalDate.parse(String.valueOf(body.get("startDate")));
        LocalDate end = LocalDate.parse(String.valueOf(body.get("endDate")));
        if (end.isBefore(start)) throw BizException.badRequest("结束日期不能早于开始日期");
        if (end.isBefore(LocalDate.now())) throw BizException.badRequest("代理结束日期不能早于今天");
        Long companyId = dataScope.companyId(user);
        List<Map<String, Object>> targetRows = companyId == null
                ? jdbc.queryForList("select user_id,status from sys_user where user_id=?", target)
                : jdbc.queryForList("select user_id,status from sys_user where user_id=? and company_id=?", target, companyId);
        if (targetRows.isEmpty() || !"0".equals(targetRows.get(0).get("status"))) {
            throw BizException.badRequest("代理审批人不可用");
        }
        Integer overlap = jdbc.queryForObject(
                "select count(*) from biz_approval_delegate where user_id=? and status='0' " +
                        "and start_date<=? and end_date>=?", Integer.class, user.getUserId(), end, start);
        if (overlap != null && overlap > 0) throw BizException.badRequest("该时间段已有审批代理");
        jdbc.update("insert into biz_approval_delegate(company_id,user_id,target_user_id,start_date,end_date,status,create_time) " +
                        "values(?,?,?,?,?,'0',now())",
                companyId, user.getUserId(), target, start, end);
    }

    public void deleteDelegate(Long id) {
        Long userId = AuthContext.require().getUserId();
        int n = jdbc.update("delete from biz_approval_delegate where delegate_id=? and user_id=?", id, userId);
        if (n == 0) throw BizException.forbidden("只能删除自己的审批代理");
    }

    public List<Map<String, Object>> rules(Long regionId) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (regionId != null && regionId <= 0) regionId = null;
        List<Map<String, Object>> base = companyId == null
                ? jdbc.queryForList("select leave_type as `leaveType`, threshold_days as `thresholdDays`, " +
                "second_level as `secondLevel` from biz_approval_rule where status='0' and company_id is null and region_id is null")
                : jdbc.queryForList("select leave_type as `leaveType`, threshold_days as `thresholdDays`, " +
                "second_level as `secondLevel` from biz_approval_rule where status='0' and company_id=? and region_id is null", companyId);
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();
        for (Map<String, Object> row : base) byType.put(String.valueOf(row.get("leaveType")), row);
        if (regionId != null) {
            List<Map<String, Object>> overrides = companyId == null
                    ? jdbc.queryForList("select leave_type as `leaveType`, threshold_days as `thresholdDays`, " +
                    "second_level as `secondLevel` from biz_approval_rule where status='0' and company_id is null and region_id=?", regionId)
                    : jdbc.queryForList("select leave_type as `leaveType`, threshold_days as `thresholdDays`, " +
                    "second_level as `secondLevel` from biz_approval_rule where status='0' and company_id=? and region_id=?", companyId, regionId);
            for (Map<String, Object> row : overrides) byType.put(String.valueOf(row.get("leaveType")), row);
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (String type : List.of("annual", "sick", "personal", "training", "temporary")) {
            Map<String, Object> row = byType.get(type);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("leaveType", type);
            item.put("thresholdDays", row == null ? 3 : row.get("thresholdDays"));
            item.put("secondLevel", row == null ? "1" : row.get("secondLevel"));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> saveRules(Long regionId, List<Map<String, Object>> rules) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (regionId != null && regionId <= 0) regionId = null;
        if (regionId != null && companyId != null) {
            List<Long> regionCompany = jdbc.queryForList(
                    "select company_id from biz_region where region_id=?", Long.class, regionId);
            if (regionCompany.isEmpty() || !companyId.equals(regionCompany.get(0))) {
                throw BizException.forbidden("只能配置本公司区域");
            }
        }
        List<Map<String, Object>> oldRows = companyId == null
                ? (regionId == null
                ? jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id is null and region_id is null")
                : jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id is null and region_id=?", regionId))
                : (regionId == null
                ? jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id=? and region_id is null", companyId)
                : jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id=? and region_id=?", companyId, regionId));
        Map<String, Map<String, Object>> oldByType = new LinkedHashMap<>();
        for (Map<String, Object> row : oldRows) oldByType.put(String.valueOf(row.get("leave_type")), row);
        if (companyId == null) {
            if (regionId == null) jdbc.update("delete from biz_approval_rule where company_id is null and region_id is null");
            else jdbc.update("delete from biz_approval_rule where company_id is null and region_id=?", regionId);
        } else {
            if (regionId == null) jdbc.update("delete from biz_approval_rule where company_id=? and region_id is null", companyId);
            else jdbc.update("delete from biz_approval_rule where company_id=? and region_id=?", companyId, regionId);
        }
        for (Map<String, Object> rule : rules == null ? List.<Map<String, Object>>of() : rules) {
            String type = String.valueOf(rule.get("leaveType"));
            if (!List.of("annual", "sick", "personal", "training", "temporary").contains(type)) continue;
            int threshold = rule.get("thresholdDays") == null ? 3 : ((Number) rule.get("thresholdDays")).intValue();
            if (threshold < 0 || threshold > 30) throw BizException.badRequest("天数阈值需在 0-30 之间");
            boolean second = "1".equals(String.valueOf(rule.get("secondLevel")))
                    || Boolean.TRUE.equals(rule.get("secondLevel"));
            jdbc.update("insert into biz_approval_rule(company_id,region_id,leave_type,threshold_days,second_level,status,update_by,update_time) " +
                            "values(?,?,?,?,?,'0',?,now())",
                    companyId, regionId, type, threshold, second ? "1" : "0", user.getNickName());
            Map<String, Object> old = oldByType.get(type);
            boolean changed = old == null
                    || ((Number) old.get("threshold_days")).intValue() != threshold
                    || !String.valueOf(old.get("second_level")).equals(second ? "1" : "0");
            if (changed) {
                jdbc.update("insert into biz_approval_rule_log(company_id,region_id,leave_type,old_threshold,old_second,new_threshold,new_second,changed_by,change_time) " +
                                "values(?,?,?,?,?,?,?,?,now())",
                        companyId, regionId, type, old == null ? null : old.get("threshold_days"),
                        old == null ? null : old.get("second_level"), threshold, second ? "1" : "0", user.getNickName());
            }
        }
        Integer versionNo = jdbc.queryForObject(
                "select coalesce(max(version_no),0)+1 from biz_approval_rule_version where " +
                        (companyId == null ? "company_id is null" : "company_id=?") +
                        " and " + (regionId == null ? "region_id is null" : "region_id=?"),
                Integer.class, versionArgs(companyId, regionId));
        if (companyId == null) {
            if (regionId == null) jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id is null and region_id is null and status='PUBLISHED'");
            else jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id is null and region_id=? and status='PUBLISHED'", regionId);
        } else {
            if (regionId == null) jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id=? and region_id is null and status='PUBLISHED'", companyId);
            else jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id=? and region_id=? and status='PUBLISHED'", companyId, regionId);
        }
        String snapshot;
        try {
            snapshot = objectMapper.writeValueAsString(rules == null ? List.of() : rules);
        } catch (Exception e) {
            throw new IllegalStateException("规则快照序列化失败", e);
        }
        jdbc.update("insert into biz_approval_rule_version(company_id,region_id,version_no,status,snapshot_json,created_by,create_time,published_by,published_time) " +
                        "values(?,?,?,'PUBLISHED',?,?,now(),?,now())",
                companyId, regionId, versionNo, snapshot, user.getNickName(), user.getNickName());
        Long versionId = jdbc.queryForObject("select last_insert_id()", Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionId", versionId);
        result.put("versionNo", versionNo);
        result.put("status", "PUBLISHED");
        return result;
    }

    private Object[] versionArgs(Long companyId, Long regionId) {
        if (companyId == null && regionId == null) return new Object[0];
        if (companyId == null) return new Object[]{regionId};
        if (regionId == null) return new Object[]{companyId};
        return new Object[]{companyId, regionId};
    }

    public List<Map<String, Object>> versions(Long regionId) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        if (regionId != null && regionId <= 0) regionId = null;
        return companyId == null
                ? jdbc.queryForList("select version_id as `versionId`, version_no as `versionNo`, status, " +
                "snapshot_json as `snapshotJson`, created_by as `createdBy`, create_time as `createTime`, published_by as `publishedBy`, " +
                "published_time as `publishedTime` from biz_approval_rule_version where company_id is null and region_id " +
                (regionId == null ? "is null" : "=?") + " order by version_no desc",
                regionId == null ? new Object[0] : new Object[]{regionId})
                : jdbc.queryForList("select version_id as `versionId`, version_no as `versionNo`, status, " +
                "snapshot_json as `snapshotJson`, created_by as `createdBy`, create_time as `createTime`, published_by as `publishedBy`, " +
                "published_time as `publishedTime` from biz_approval_rule_version where company_id=? and region_id " +
                (regionId == null ? "is null" : "=?") + " order by version_no desc",
                regionId == null ? new Object[]{companyId} : new Object[]{companyId, regionId});
    }

    @Transactional
    public void publishVersion(Long versionId) {
        LoginUser user = AuthContext.require();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from biz_approval_rule_version where version_id=?", versionId);
        if (rows.isEmpty()) throw BizException.badRequest("版本不存在");
        Map<String, Object> row = rows.get(0);
        Long ownerCompany = row.get("company_id") == null ? null : ((Number) row.get("company_id")).longValue();
        if (!user.isSuperAdmin()) {
            Long currentCompany = dataScope.companyId(user);
            if (currentCompany == null || !currentCompany.equals(ownerCompany)) {
                throw BizException.forbidden("无权发布其他公司的规则版本");
            }
        }
        Long regionId = row.get("region_id") == null ? null : ((Number) row.get("region_id")).longValue();
        List<Map<String, Object>> rules = parseSnapshot(String.valueOf(row.get("snapshot_json")));
        applySnapshot(ownerCompany, regionId, rules, user);
        if (ownerCompany == null) {
            if (regionId == null) jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id is null and region_id is null and status='PUBLISHED'");
            else jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id is null and region_id=? and status='PUBLISHED'", regionId);
        } else {
            if (regionId == null) jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id=? and region_id is null and status='PUBLISHED'", ownerCompany);
            else jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id=? and region_id=? and status='PUBLISHED'", ownerCompany, regionId);
        }
        jdbc.update("update biz_approval_rule_version set status='PUBLISHED',published_by=?,published_time=now() where version_id=?",
                user.getNickName(), versionId);
    }

    @Transactional
    public Map<String, Object> rollbackVersion(Long versionId) {
        LoginUser user = AuthContext.require();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select * from biz_approval_rule_version where version_id=?", versionId);
        if (rows.isEmpty()) throw BizException.badRequest("版本不存在");
        Map<String, Object> row = rows.get(0);
        Long ownerCompany = row.get("company_id") == null ? null : ((Number) row.get("company_id")).longValue();
        if (!user.isSuperAdmin()) {
            Long currentCompany = dataScope.companyId(user);
            if (currentCompany == null || !currentCompany.equals(ownerCompany)) {
                throw BizException.forbidden("无权回滚其他公司的规则版本");
            }
        }
        Long regionId = row.get("region_id") == null ? null : ((Number) row.get("region_id")).longValue();
        List<Map<String, Object>> rules = parseSnapshot(String.valueOf(row.get("snapshot_json")));
        applySnapshot(ownerCompany, regionId, rules, user);
        return createVersion(ownerCompany, regionId, rules, user);
    }

    private Map<String, Object> createVersion(Long companyId, Long regionId, List<Map<String, Object>> rules, LoginUser user) {
        Integer versionNo = jdbc.queryForObject(
                "select coalesce(max(version_no),0)+1 from biz_approval_rule_version where " +
                        (companyId == null ? "company_id is null" : "company_id=?") +
                        " and " + (regionId == null ? "region_id is null" : "region_id=?"),
                Integer.class, versionArgs(companyId, regionId));
        if (companyId == null) {
            if (regionId == null) jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id is null and region_id is null and status='PUBLISHED'");
            else jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id is null and region_id=? and status='PUBLISHED'", regionId);
        } else {
            if (regionId == null) jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id=? and region_id is null and status='PUBLISHED'", companyId);
            else jdbc.update("update biz_approval_rule_version set status='ARCHIVED' where company_id=? and region_id=? and status='PUBLISHED'", companyId, regionId);
        }
        String snapshot;
        try {
            snapshot = objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            throw new IllegalStateException("规则快照序列化失败", e);
        }
        jdbc.update("insert into biz_approval_rule_version(company_id,region_id,version_no,status,snapshot_json,created_by,create_time,published_by,published_time) " +
                        "values(?,?,?,'PUBLISHED',?,?,now(),?,now())",
                companyId, regionId, versionNo, snapshot, user.getNickName(), user.getNickName());
        Long newId = jdbc.queryForObject("select last_insert_id()", Long.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionId", newId);
        result.put("versionNo", versionNo);
        result.put("status", "PUBLISHED");
        return result;
    }

    private List<Map<String, Object>> parseSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw BizException.badRequest("版本快照解析失败");
        }
    }

    private void applySnapshot(Long companyId, Long regionId, List<Map<String, Object>> rules, LoginUser user) {
        List<Map<String, Object>> oldRows = companyId == null
                ? (regionId == null
                ? jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id is null and region_id is null")
                : jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id is null and region_id=?", regionId))
                : (regionId == null
                ? jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id=? and region_id is null", companyId)
                : jdbc.queryForList("select leave_type, threshold_days, second_level from biz_approval_rule where company_id=? and region_id=?", companyId, regionId));
        Map<String, Map<String, Object>> oldByType = new LinkedHashMap<>();
        for (Map<String, Object> r : oldRows) oldByType.put(String.valueOf(r.get("leave_type")), r);
        if (companyId == null) {
            if (regionId == null) jdbc.update("delete from biz_approval_rule where company_id is null and region_id is null");
            else jdbc.update("delete from biz_approval_rule where company_id is null and region_id=?", regionId);
        } else {
            if (regionId == null) jdbc.update("delete from biz_approval_rule where company_id=? and region_id is null", companyId);
            else jdbc.update("delete from biz_approval_rule where company_id=? and region_id=?", companyId, regionId);
        }
        for (Map<String, Object> rule : rules) {
            String type = String.valueOf(rule.get("leaveType"));
            if (!List.of("annual", "sick", "personal", "training", "temporary").contains(type)) continue;
            int threshold = rule.get("thresholdDays") == null ? 3 : ((Number) rule.get("thresholdDays")).intValue();
            boolean second = "1".equals(String.valueOf(rule.get("secondLevel")))
                    || Boolean.TRUE.equals(rule.get("secondLevel"));
            jdbc.update("insert into biz_approval_rule(company_id,region_id,leave_type,threshold_days,second_level,status,update_by,update_time) " +
                            "values(?,?,?,?,?,'0',?,now())",
                    companyId, regionId, type, threshold, second ? "1" : "0", user.getNickName());
            Map<String, Object> old = oldByType.get(type);
            if (old == null || ((Number) old.get("threshold_days")).intValue() != threshold
                    || !String.valueOf(old.get("second_level")).equals(second ? "1" : "0")) {
                jdbc.update("insert into biz_approval_rule_log(company_id,region_id,leave_type,old_threshold,old_second,new_threshold,new_second,changed_by,change_time) " +
                                "values(?,?,?,?,?,?,?,?,now())",
                        companyId, regionId, type, old == null ? null : old.get("threshold_days"),
                        old == null ? null : old.get("second_level"), threshold, second ? "1" : "0", user.getNickName());
            }
        }
    }

    public List<Map<String, Object>> rulesHistory(Long regionId) {        Long companyId = dataScope.companyId(AuthContext.require());
        if (regionId != null && regionId <= 0) regionId = null;
        return companyId == null
                ? jdbc.queryForList("select log_id as `logId`, leave_type as `leaveType`, old_threshold as `oldThreshold`, " +
                "old_second as `oldSecond`, new_threshold as `newThreshold`, new_second as `newSecond`, " +
                "changed_by as `changedBy`, change_time as `changeTime` from biz_approval_rule_log " +
                "where company_id is null and region_id " + (regionId == null ? "is null" : "=?") +
                " order by log_id desc limit 50", regionId == null ? new Object[0] : new Object[]{regionId})
                : jdbc.queryForList("select log_id as `logId`, leave_type as `leaveType`, old_threshold as `oldThreshold`, " +
                "old_second as `oldSecond`, new_threshold as `newThreshold`, new_second as `newSecond`, " +
                "changed_by as `changedBy`, change_time as `changeTime` from biz_approval_rule_log " +
                "where company_id=? and region_id " + (regionId == null ? "is null" : "=?") +
                " order by log_id desc limit 50",
                regionId == null ? new Object[]{companyId} : new Object[]{companyId, regionId});
    }

    public void rollbackRule(Long logId) {
        LoginUser user = AuthContext.require();
        Long companyId = dataScope.companyId(user);
        List<Map<String, Object>> logs = companyId == null
                ? jdbc.queryForList("select * from biz_approval_rule_log where log_id=? and company_id is null", logId)
                : jdbc.queryForList("select * from biz_approval_rule_log where log_id=? and company_id=?", logId, companyId);
        if (logs.isEmpty()) throw BizException.badRequest("变更记录不存在");
        Map<String, Object> log = logs.get(0);
        Object oldThreshold = log.get("old_threshold");
        Object oldSecond = log.get("old_second");
        if (oldThreshold == null) throw BizException.badRequest("该记录没有可回滚的旧配置");
        String type = String.valueOf(log.get("leave_type"));
        Long regionId = log.get("region_id") == null ? null : ((Number) log.get("region_id")).longValue();
        int newThreshold = ((Number) oldThreshold).intValue();
        String newSecond = oldSecond == null ? "0" : String.valueOf(oldSecond);
        int oldThresholdNow = 3;
        String oldSecondNow = "1";
        List<Map<String, Object>> current = companyId == null
                ? (regionId == null
                ? jdbc.queryForList("select threshold_days,second_level from biz_approval_rule where company_id is null and region_id is null and leave_type=?", type)
                : jdbc.queryForList("select threshold_days,second_level from biz_approval_rule where company_id is null and region_id=? and leave_type=?", regionId, type))
                : (regionId == null
                ? jdbc.queryForList("select threshold_days,second_level from biz_approval_rule where company_id=? and region_id is null and leave_type=?", companyId, type)
                : jdbc.queryForList("select threshold_days,second_level from biz_approval_rule where company_id=? and region_id=? and leave_type=?", companyId, regionId, type));
        if (!current.isEmpty()) {
            oldThresholdNow = ((Number) current.get(0).get("threshold_days")).intValue();
            oldSecondNow = String.valueOf(current.get(0).get("second_level"));
            if (companyId == null) {
                if (regionId == null) jdbc.update("update biz_approval_rule set threshold_days=?,second_level=?,update_by=?,update_time=now() where company_id is null and region_id is null and leave_type=?", newThreshold, newSecond, user.getNickName(), type);
                else jdbc.update("update biz_approval_rule set threshold_days=?,second_level=?,update_by=?,update_time=now() where company_id is null and region_id=? and leave_type=?", newThreshold, newSecond, user.getNickName(), regionId, type);
            } else {
                if (regionId == null) jdbc.update("update biz_approval_rule set threshold_days=?,second_level=?,update_by=?,update_time=now() where company_id=? and region_id is null and leave_type=?", newThreshold, newSecond, user.getNickName(), companyId, type);
                else jdbc.update("update biz_approval_rule set threshold_days=?,second_level=?,update_by=?,update_time=now() where company_id=? and region_id=? and leave_type=?", newThreshold, newSecond, user.getNickName(), companyId, regionId, type);
            }
        } else {
            jdbc.update("insert into biz_approval_rule(company_id,region_id,leave_type,threshold_days,second_level,status,update_by,update_time) " +
                    "values(?,?,?,?,?,'0',?,now())", companyId, regionId, type, newThreshold, newSecond, user.getNickName());
        }
        jdbc.update("insert into biz_approval_rule_log(company_id,region_id,leave_type,old_threshold,old_second,new_threshold,new_second,changed_by,change_time) " +
                        "values(?,?,?,?,?,?,?,?,now())",
                companyId, regionId, type, oldThresholdNow, oldSecondNow, newThreshold, newSecond, user.getNickName());
    }

    public int escalateDelegateExpiry() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select d.delegate_id, d.company_id, d.user_id, d.target_user_id, d.end_date, " +
                        "u.nick_name as `userName`, t.nick_name as `targetName` " +
                        "from biz_approval_delegate d join sys_user u on u.user_id=d.user_id " +
                        "left join sys_user t on t.user_id=d.target_user_id " +
                        "where d.status='0' and d.end_date between curdate() " +
                        "and date_add(curdate(), interval ? day)", delegateRemindDays);
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long delegateId = ((Number) row.get("delegate_id")).longValue();
            Long userId = ((Number) row.get("user_id")).longValue();
            Long companyId = row.get("company_id") == null ? null : ((Number) row.get("company_id")).longValue();
            count += jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                            "receiver_role,company_id,is_read,create_time) values('approval',?,?,'2','approval_delegate',?,?,?,null,?,'0',now())",
                    "审批代理即将到期", "你设置给 " + row.get("targetName") + " 的审批代理将于 " + row.get("end_date") + " 到期",
                    delegateId, "delegate:expiry:" + delegateId + ":" + LocalDate.now() + ":" + userId, userId, companyId);
        }
        return count;
    }

    public int escalateTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(timeoutHours);
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "select t.task_id as `taskId`, t.approver_id as `approverId`, t.step_no as `stepNo`, " +
                        "l.leave_id as `leaveId`, l.company_id as `companyId`, e.engineer_name as `engineerName`, " +
                        "l.start_date as `startDate`, l.end_date as `endDate` " +
                        "from biz_approval_task t join biz_engineer_leave l on l.leave_id=t.biz_id " +
                        "join biz_engineer e on e.engineer_id=l.engineer_id " +
                        "where t.biz_type='engineer_leave' and t.status='PENDING' and t.create_time < ?",
                cutoff);
        int count = 0;
        for (Map<String, Object> task : tasks) {
            Long taskId = ((Number) task.get("taskId")).longValue();
            Long approver = ((Number) task.get("approverId")).longValue();
            Long companyId = task.get("companyId") == null ? null : ((Number) task.get("companyId")).longValue();
            String dateKey = LocalDate.now().toString();
            Set<Long> receivers = new LinkedHashSet<>();
            receivers.add(approver);
            List<Long> managers = companyId == null ? List.of() : jdbc.queryForList(
                    "select distinct u.user_id from sys_user u join sys_user_role ur on ur.user_id=u.user_id " +
                            "join sys_role r on r.role_id=ur.role_id where u.company_id=? and u.status='0' " +
                            "and r.role_key='maintenance_manager' order by u.user_id", Long.class, companyId);
            receivers.addAll(managers);
            for (Long receiver : receivers) {
                count += jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                                "receiver_role,company_id,is_read,create_time) values('approval',?,?,'2','approval_task',?,?,?,null,?,'0',now())",
                        "审批超时提醒",
                        task.get("engineerName") + " 的请假审批已超过 " + timeoutHours + " 小时未处理",
                        taskId, "approval:timeout:" + taskId + ":" + dateKey + ":" + receiver,
                        receiver, companyId);
            }
        }
        return count;
    }
}
