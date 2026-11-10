package com.laminar.ops.biz.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;

    @Value("${laminar.leave.long-threshold-days:3}")
    private int longThresholdDays;

    @Value("${laminar.leave.handover-timeout-hours:24}")
    private int handoverTimeoutHours;

    @Transactional
    public Long apply(Map<String, Object> body) {
        LoginUser user = AuthContext.require();
        Long engineerId = user.getEngineerId();
        if (engineerId == null) throw BizException.badRequest("当前账号未绑定工程师，不能提交请假");
        Map<String, Object> engineer = jdbc.queryForMap(
                "select engineer_id, engineer_name, company_id, region_id from biz_engineer where engineer_id=?", engineerId);
        Long companyId = engineer.get("company_id") == null ? user.getCompanyId()
                : ((Number) engineer.get("company_id")).longValue();
        boolean regionalManager = user.getRoleKeys().contains("regional_manager");
        boolean engineerRole = user.getRoleKeys().contains("engineer");
        Long totalManagerId = companyTotalManager(companyId, user.getUserId());
        boolean totalManager = user.getUserId().equals(totalManagerId);
        if (!totalManager && !regionalManager && !engineerRole) {
            throw BizException.badRequest("当前账号不属于维保中心，不能提交请假");
        }
        LocalDate start = LocalDate.parse(str(body.get("startDate")));
        LocalDate end = LocalDate.parse(str(body.get("endDate")));
        if (end.isBefore(start)) throw BizException.badRequest("结束日期不能早于开始日期");
        if (start.isBefore(LocalDate.now())) throw BizException.badRequest("请假开始日期不能早于今天");
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        Integer overlap = jdbc.queryForObject(
                "select count(*) from biz_engineer_leave where engineer_id=? " +
                        "and status in ('PENDING_REGION','PENDING_COMPANY','APPROVED') " +
                        "and start_date <= ? and end_date >= ?",
                Integer.class, engineerId, end, start);
        if (overlap != null && overlap > 0) throw BizException.badRequest("该时间段已有请假申请");

        String status;
        int currentStep = 1;
        Long approver = null;
        boolean needSecond = false;
        List<Long> chain = new ArrayList<>();
        Object regionId = engineer.get("region_id");
        if (totalManager) {
            status = "APPROVED";
        } else {
            if (regionalManager) {
                Long managedRegion = jdbc.queryForList(
                                "select region_id from biz_region where manager_id=? and company_id=? order by region_id limit 1",
                                Long.class, user.getUserId(), companyId)
                        .stream().findFirst().orElse(regionId == null ? null : ((Number) regionId).longValue());
                if (managedRegion == null) throw BizException.badRequest("未配置所属区域，无法确定审批链");
                chain = regionManagerChain(managedRegion, companyId, user.getUserId());
            } else {
                if (regionId == null) {
                    throw BizException.badRequest("所属区域未配置，无法确定审批人，请先完善工程师档案");
                }
                needSecond = needSecond(companyId, ((Number) regionId).longValue(),
                        str(body.getOrDefault("leaveType", "personal")), days);
                chain = engineerChain(((Number) regionId).longValue(), companyId, user.getUserId(), needSecond);
            }
            if (totalManagerId != null && !chain.contains(totalManagerId) && !totalManagerId.equals(user.getUserId())) {
                Long resolvedTotal = delegated(totalManagerId, LocalDate.now());
                if (resolvedTotal != null && !resolvedTotal.equals(user.getUserId()) && !chain.contains(resolvedTotal)) {
                    chain.add(resolvedTotal);
                }
            }
            if (chain.isEmpty()) throw BizException.badRequest("未配置可用审批人，不能提交请假");
            approver = chain.get(0);
            needSecond = chain.size() > 1 || needSecond;
            status = (chain.size() == 1 && approver.equals(totalManagerId)) ? "PENDING_COMPANY" : "PENDING_REGION";
        }

        KeyHolder holder = new GeneratedKeyHolder();
        final String insertStatus = status;
        final int insertStep = currentStep;
        final boolean insertNeedSecond = needSecond;
        final String chainText = chain.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into biz_engineer_leave(engineer_id,company_id,leave_type,start_date,end_date,days,need_second,reason," +
                            "status,current_step,approver_chain,created_by,create_time,update_time) " +
                            "values(?,?,?,?,?,?,?,?,?,?,?,?,now(),now())", Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, engineerId);
            ps.setObject(2, companyId);
            ps.setString(3, str(body.getOrDefault("leaveType", "personal")));
            ps.setObject(4, start);
            ps.setObject(5, end);
            ps.setInt(6, days);
            ps.setInt(7, insertNeedSecond ? 1 : 0);
            ps.setString(8, str(body.get("reason")));
            ps.setString(9, insertStatus);
            ps.setInt(10, insertStep);
            ps.setString(11, chainText);
            ps.setObject(12, user.getUserId());
            return ps;
        }, holder);
        Long leaveId = holder.getKey() == null ? 0L : holder.getKey().longValue();
        if (totalManager) return leaveId;
        createTask(leaveId, currentStep, approver);
        notifyLeave(approver, leaveId, "submit",
                status.equals("PENDING_COMPANY") ? "请假待总区域审批" : "请假待审批",
                engineer.get("engineer_name") + " 提交了请假申请", companyId);
        return leaveId;
    }

    public List<Map<String, Object>> my() {
        LoginUser user = AuthContext.require();
        if (user.getEngineerId() == null) return List.of();
        return leaveRows(" where l.engineer_id = ? order by l.leave_id desc", List.of(user.getEngineerId()));
    }

    public List<Map<String, Object>> pending() {
        LoginUser user = AuthContext.require();
        return jdbc.queryForList(
                "select t.task_id as `taskId`, t.step_no as `stepNo`, l.leave_id as `leaveId`, " +
                        "e.engineer_name as `engineerName`, l.leave_type as `leaveType`, l.start_date as `startDate`, " +
                        "l.end_date as `endDate`, l.days, l.reason, l.status " +
                        "from biz_approval_task t join biz_engineer_leave l on l.leave_id=t.biz_id " +
                        "join biz_engineer e on e.engineer_id=l.engineer_id " +
                        "where t.biz_type='engineer_leave' and t.approver_id=? and t.status='PENDING' " +
                        "order by t.create_time", user.getUserId());
    }

    public Map<String, Object> page(int pageNum, int pageSize, Map<String, String> params) {
        LoginUser user = AuthContext.require();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            where.append(" and l.company_id=? ");
            args.add(companyId);
        }
        if ("self".equals(user.getDataScope()) && user.getEngineerId() != null) {
            where.append(" and l.engineer_id=? ");
            args.add(user.getEngineerId());
        }
        Long total = jdbc.queryForObject(
                "select count(*) from biz_engineer_leave l" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(Math.max(0, (pageNum - 1) * pageSize));
        List<Map<String, Object>> records = leaveRows(where.toString() + " order by l.leave_id desc limit ? offset ?", pageArgs);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    @Transactional
    public void approve(Long id, String comment, boolean approved) {
        LoginUser user = AuthContext.require();
        List<Map<String, Object>> tasks = jdbc.queryForList(
                "select task_id, step_no from biz_approval_task where biz_type='engineer_leave' and biz_id=? " +
                        "and approver_id=? and status='PENDING' order by step_no limit 1", id, user.getUserId());
        if (tasks.isEmpty()) throw BizException.forbidden("当前用户不是该请假的待审批人");
        Map<String, Object> task = tasks.get(0);
        Long taskId = ((Number) task.get("task_id")).longValue();
        int step = ((Number) task.get("step_no")).intValue();
        int updated = jdbc.update("update biz_approval_task set status=?,action=?,comment=?,acted_at=now() " +
                        "where task_id=? and status='PENDING'",
                approved ? "APPROVED" : "REJECTED", approved ? "approve" : "reject", comment, taskId);
        if (updated == 0) {
            throw BizException.badRequest("该审批任务已被处理，请刷新后重试");
        }
        Map<String, Object> leave = jdbc.queryForMap(
                "select l.*, e.engineer_name, e.region_id, e.company_id, e.user_id as engineer_user_id " +
                        "from biz_engineer_leave l join biz_engineer e on e.engineer_id=l.engineer_id where l.leave_id=?", id);
        Long engineerUserId = leave.get("engineer_user_id") == null ? null
                : ((Number) leave.get("engineer_user_id")).longValue();
        if (!approved) {
            jdbc.update("update biz_engineer_leave set status='REJECTED',update_time=now() where leave_id=?", id);
            jdbc.update("update biz_approval_task set status='CANCELLED' where biz_type='engineer_leave' and biz_id=? and status='PENDING'", id);
            notifyLeave(engineerUserId, id, "rejected", "请假已驳回",
                    "你的请假申请已被驳回：" + (comment == null ? "" : comment),
                    leave.get("company_id") == null ? null : ((Number) leave.get("company_id")).longValue());
            return;
        }
        int days = ((Number) leave.get("days")).intValue();
        Object chainObj = leave.get("approver_chain");
        if (chainObj != null && !String.valueOf(chainObj).isBlank()) {
            String[] parts = String.valueOf(chainObj).split(",");
            int nextIndex = step;
            if (nextIndex < parts.length) {
                Long nextApprover = Long.valueOf(parts[nextIndex]);
                boolean last = nextIndex + 1 == parts.length;
                createTask(id, step + 1, nextApprover);
                jdbc.update("update biz_engineer_leave set status=?,current_step=?,update_time=now() where leave_id=?",
                        last ? "PENDING_COMPANY" : "PENDING_REGION", step + 1, id);
                notifyLeave(nextApprover, id, "step" + (step + 1),
                        last ? "请假待总区域审批" : "请假待上级审批",
                        leave.get("engineer_name") + " 的请假已进入第 " + (step + 1) + " 级审批",
                        leave.get("company_id") == null ? null : ((Number) leave.get("company_id")).longValue());
                return;
            }
        } else if (step == 1 && "1".equals(String.valueOf(leave.get("need_second")))) {
            Long approver = secondApprover(leave, user.getUserId());
            if (approver == null) {
                throw BizException.badRequest("未配置总区域负责人，无法完成审批");
            }
            Long firstApprover = jdbc.queryForObject(
                    "select approver_id from biz_approval_task where biz_type='engineer_leave' and biz_id=? and step_no=1",
                    Long.class, id);
            if (approver.equals(firstApprover)) {
                throw BizException.badRequest("二级审批人与一级审批人相同，请先配置上级区域负责人");
            }
            createTask(id, 2, approver);
            jdbc.update("update biz_engineer_leave set status='PENDING_COMPANY',current_step=2,update_time=now() where leave_id=?", id);
            notifyLeave(approver, id, "step2", "请假待总区域审批",
                    leave.get("engineer_name") + " 的请假已通过区域审批，等待你的审批",
                    leave.get("company_id") == null ? null : ((Number) leave.get("company_id")).longValue());
            return;
        }
        Long engineerId = ((Number) leave.get("engineer_id")).longValue();
        Integer active = jdbc.queryForObject(
                "select count(*) from biz_workorder where engineer_id=? and status in ('0','1','2','3','4')",
                Integer.class, engineerId);
        if (active != null && active > 0) {
            jdbc.update("insert ignore into biz_leave_handover(leave_id,engineer_id,workorder_id,status,create_time) " +
                            "select ?, engineer_id, workorder_id, 'PENDING', now() from biz_workorder " +
                            "where engineer_id=? and status in ('0','1','2','3','4')",
                    id, engineerId);
            jdbc.update("update biz_engineer_leave set status='PENDING_HANDOVER',current_step=3,update_time=now() where leave_id=?", id);
            notifyLeave(engineerUserId, id, "handover", "请假待交接",
                    "请假已通过审批，但还有 " + active + " 张在手单需要改派", leave.get("company_id") == null ? null : ((Number) leave.get("company_id")).longValue());
            return;
        }
        jdbc.update("update biz_engineer_leave set status='APPROVED',current_step=?,update_time=now() where leave_id=?",
                step, id);
        notifyLeave(engineerUserId, id, "approved", "请假已批准",
                "你的请假申请已审批通过", leave.get("company_id") == null ? null : ((Number) leave.get("company_id")).longValue());
    }

    @Transactional
    public void cancel(Long id) {
        LoginUser user = AuthContext.require();
        Map<String, Object> leave = jdbc.queryForMap(
                "select engineer_id, created_by, status from biz_engineer_leave where leave_id=?", id);
        boolean owner = user.getEngineerId() != null
                && user.getEngineerId().equals(((Number) leave.get("engineer_id")).longValue());
        boolean creator = leave.get("created_by") != null
                && user.getUserId().equals(((Number) leave.get("created_by")).longValue());
        if (!owner && !creator) throw BizException.forbidden("只能取消自己的请假");
        if ("APPROVED".equals(leave.get("status")) || "CANCELLED".equals(leave.get("status"))) {
            throw BizException.badRequest("该请假已结束，不能取消");
        }
        jdbc.update("update biz_engineer_leave set status='CANCELLED',update_time=now() where leave_id=?", id);
        jdbc.update("update biz_approval_task set status='CANCELLED' where biz_type='engineer_leave' and biz_id=? and status='PENDING'", id);
    }

    public boolean onApprovedLeave(Long engineerId, LocalDate date) {
        Integer count = jdbc.queryForObject(
                "select count(*) from biz_engineer_leave where engineer_id=? and status='APPROVED' " +
                        "and start_date <= ? and end_date >= ?", Integer.class, engineerId, date, date);
        return count != null && count > 0;
    }

    private boolean needSecond(Long companyId, Long regionId, String leaveType, int days) {
        List<Map<String, Object>> rows = null;
        if (regionId != null) {
            rows = jdbc.queryForList(
                    "select threshold_days as `thresholdDays`, second_level as `secondLevel` from biz_approval_rule " +
                            "where leave_type=? and status='0' and company_id=? and region_id=? order by rule_id limit 1",
                    leaveType, companyId, regionId);
        }
        if (rows == null || rows.isEmpty()) {
            rows = jdbc.queryForList(
                    "select threshold_days as `thresholdDays`, second_level as `secondLevel` from biz_approval_rule " +
                            "where leave_type=? and status='0' and company_id=? and region_id is null order by rule_id limit 1",
                    leaveType, companyId);
        }
        if (rows.isEmpty() && companyId != null) {
            rows = jdbc.queryForList(
                    "select threshold_days as `thresholdDays`, second_level as `secondLevel` from biz_approval_rule " +
                            "where leave_type=? and status='0' and company_id is null and region_id is null order by rule_id limit 1",
                    leaveType);
        }
        if (rows.isEmpty()) return days > longThresholdDays;
        int threshold = ((Number) rows.get(0).get("thresholdDays")).intValue();
        boolean second = "1".equals(String.valueOf(rows.get(0).get("secondLevel")));
        return second && days > threshold;
    }

    public int escalateHandoverTimeouts() {        LocalDateTime cutoff = LocalDateTime.now().minusHours(handoverTimeoutHours);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select h.handover_id, h.leave_id, h.workorder_id, l.company_id, e.engineer_name, e.user_id " +
                        "from biz_leave_handover h join biz_engineer_leave l on l.leave_id=h.leave_id " +
                        "join biz_engineer e on e.engineer_id=l.engineer_id " +
                        "where h.status='PENDING' and h.create_time < ?", cutoff);
        int count = 0;
        for (Map<String, Object> row : rows) {
            Long leaveId = ((Number) row.get("leave_id")).longValue();
            Long handoverId = ((Number) row.get("handover_id")).longValue();
            Long companyId = row.get("company_id") == null ? null : ((Number) row.get("company_id")).longValue();
            Set<Long> receivers = new LinkedHashSet<>();
            if (row.get("user_id") != null) receivers.add(((Number) row.get("user_id")).longValue());
            if (companyId != null) {
                receivers.addAll(jdbc.queryForList(
                        "select distinct u.user_id from sys_user u join sys_user_role ur on ur.user_id=u.user_id " +
                                "join sys_role r on r.role_id=ur.role_id where u.company_id=? and u.status='0' " +
                                "and r.role_key='maintenance_manager'", Long.class, companyId));
            }
            for (Long receiver : receivers) {
                count += jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                                "receiver_role,company_id,is_read,create_time) values('leave',?,?,'2','engineer_leave',?,?,?,null,?,'0',now())",
                        "改派超时提醒",
                        row.get("engineer_name") + " 的请假还有待改派工单超过 " + handoverTimeoutHours + " 小时",
                        leaveId, "leave:" + leaveId + ":handover-timeout:" + handoverId + ":" + LocalDate.now() + ":" + receiver,
                        receiver, companyId);
            }
        }
        return count;
    }

    private List<Map<String, Object>> leaveRows(String suffix, List<Object> args) {
        return jdbc.queryForList(
                "select l.leave_id as `leaveId`, l.engineer_id as `engineerId`, e.engineer_name as `engineerName`, " +
                        "l.leave_type as `leaveType`, l.start_date as `startDate`, l.end_date as `endDate`, " +
                        "l.days, l.reason, l.status, l.current_step as `currentStep`, l.create_time as `createTime` " +
                        "from biz_engineer_leave l join biz_engineer e on e.engineer_id=l.engineer_id " + suffix,
                args.toArray());
    }

    private Long companyTotalManager(Long companyId, Long applicant) {
        List<Long> ids = jdbc.queryForList(
                "select c.total_manager_id from biz_company c join sys_user u on u.user_id=c.total_manager_id " +
                        "where c.company_id=? and u.status='0' and u.company_id=?", Long.class, companyId, companyId);
        if (!ids.isEmpty() && !ids.get(0).equals(applicant)) return ids.get(0);
        if (!ids.isEmpty()) return ids.get(0);
        return companyManager(companyId, applicant);
    }

    private List<Long> engineerChain(Long regionId, Long companyId, Long applicant, boolean longLeave) {
        List<Long> managers = ancestorManagers(regionId, companyId);
        List<Long> chain = new ArrayList<>();
        if (managers.isEmpty()) return chain;
        if (longLeave) {
            for (Long manager : managers) addApprover(chain, manager, applicant);
        } else {
            addApprover(chain, managers.get(0), applicant);
        }
        return chain;
    }

    private List<Long> regionManagerChain(Long managedRegionId, Long companyId, Long applicant) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select parent_id from biz_region where region_id=?", managedRegionId);
        if (rows.isEmpty() || rows.get(0).get("parent_id") == null) return new ArrayList<>();
        Long parentId = ((Number) rows.get(0).get("parent_id")).longValue();
        List<Long> chain = new ArrayList<>();
        for (Long manager : ancestorManagers(parentId, companyId)) {
            addApprover(chain, manager, applicant);
        }
        return chain;
    }

    private List<Long> ancestorManagers(Long startRegionId, Long companyId) {
        List<Long> result = new ArrayList<>();
        Long current = startRegionId;
        int guard = 0;
        while (current != null && guard++ < 20) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select r.parent_id, r.manager_id, u.status as `managerStatus`, u.company_id as `managerCompany` " +
                            "from biz_region r left join sys_user u on u.user_id=r.manager_id where r.region_id=?", current);
            if (rows.isEmpty()) break;
            Map<String, Object> row = rows.get(0);
            Object managerId = row.get("manager_id");
            if (managerId != null && "0".equals(String.valueOf(row.get("managerStatus")))
                    && row.get("managerCompany") != null
                    && ((Number) row.get("managerCompany")).longValue() == companyId.longValue()) {
                result.add(((Number) managerId).longValue());
            }
            Object parent = row.get("parent_id");
            current = parent == null ? null : ((Number) parent).longValue();
        }
        return result;
    }

    private void addApprover(List<Long> chain, Long managerId, Long applicant) {
        if (managerId == null || managerId.equals(applicant)) return;
        Long resolved = delegated(managerId, LocalDate.now());
        if (resolved == null || resolved.equals(applicant) || chain.contains(resolved)) return;
        chain.add(resolved);
    }

    private Long firstApprover(Map<String, Object> engineer, Long companyId, Long applicant) {
        Object regionId = engineer.get("region_id");
        if (regionId != null) {
            List<Long> ids = jdbc.queryForList(
                    "select manager_id from biz_region where region_id=? and manager_id is not null", Long.class, regionId);
            if (!ids.isEmpty() && !ids.get(0).equals(applicant)) return delegated(ids.get(0), LocalDate.now());
        }
        return delegated(companyManager(companyId, applicant), LocalDate.now());
    }

    private Long secondApprover(Map<String, Object> leave, Long applicant) {
        Object regionId = leave.get("region_id");
        if (regionId != null) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select r.parent_id, p.manager_id from biz_region r left join biz_region p on p.region_id=r.parent_id " +
                            "where r.region_id=?", regionId);
            if (!rows.isEmpty() && rows.get(0).get("manager_id") != null) {
                Long id = ((Number) rows.get(0).get("manager_id")).longValue();
                if (!id.equals(applicant)) return delegated(id, LocalDate.now());
            }
        }
        Object company = leave.get("company_id");
        return company == null ? null : delegated(companyManager(((Number) company).longValue(), applicant), LocalDate.now());
    }

    private Long delegated(Long userId, LocalDate date) {
        if (userId == null) return null;
        List<Long> ids = jdbc.queryForList(
                "select target_user_id from biz_approval_delegate where user_id=? and status='0' " +
                        "and start_date<=? and end_date>=? order by delegate_id limit 1",
                Long.class, userId, date, date);
        return ids.isEmpty() ? userId : ids.get(0);
    }

    private Long companyManager(Long companyId, Long applicant) {
        List<Long> ids = jdbc.queryForList(
                "select distinct u.user_id from sys_user u join sys_user_role ur on ur.user_id=u.user_id " +
                        "join sys_role r on r.role_id=ur.role_id where u.company_id=? and u.status='0' " +
                        "and r.role_key='maintenance_manager' and u.user_id<>? order by u.user_id limit 1",
                Long.class, companyId, applicant);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void createTask(Long leaveId, int step, Long approverId) {
        jdbc.update("insert into biz_approval_task(biz_type,biz_id,step_no,approver_id,status,create_time) " +
                        "values('engineer_leave',?,?,?,'PENDING',now())",
                leaveId, step, approverId);
    }

    private void notifyLeave(Long userId, Long leaveId, String keyPart, String title, String content, Long companyId) {
        if (userId == null) return;
        jdbc.update("insert ignore into biz_message(msg_type,title,content,level,biz_type,biz_id,biz_key,receiver_id," +
                        "receiver_role,company_id,is_read,create_time) " +
                        "values('leave',?,?,'2','engineer_leave',?,?,?,null,?,'0',now())",
                title, content, leaveId, "leave:" + leaveId + ":" + keyPart + ":" + userId, userId, companyId);
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }
}
