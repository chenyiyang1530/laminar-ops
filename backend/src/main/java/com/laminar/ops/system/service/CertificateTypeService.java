package com.laminar.ops.system.service;

import com.laminar.ops.common.BizException;
import com.laminar.ops.common.crud.ResourceDef;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CertificateTypeService {

    private final JdbcTemplate jdbc;

    /** 已有字典类型直接复用；未知类型保留原始值。 */
    public void normalize(ResourceDef def, Map<String, Object> body) {
        if (!"certificate".equals(def.getKey())) return;
        Object raw = body.get("certType");
        if (raw == null || String.valueOf(raw).isBlank()) return;
        String value = String.valueOf(raw).trim();
        List<Map<String, Object>> dict = jdbc.queryForList(
                "select dict_value as `value` from sys_dict_data " +
                        "where dict_type='cert_type' and status='0' and (dict_value=? or dict_label=?) limit 1",
                value, value);
        if (!dict.isEmpty()) body.put("certType", String.valueOf(dict.get(0).get("value")));
        else body.put("certType", value);
    }

    /** 证书保存成功后再登记新类型申请，避免保存失败留下脏申请。 */
    public void recordRequest(ResourceDef def, Map<String, Object> body, LoginUser user) {
        if (!"certificate".equals(def.getKey())) return;
        Object raw = body.get("certType");
        if (raw == null || String.valueOf(raw).isBlank()) return;
        String value = String.valueOf(raw).trim();
        Integer existsDict = jdbc.queryForObject(
                "select count(*) from sys_dict_data where dict_type='cert_type' and (dict_value=? or dict_label=?)",
                Integer.class, value, value);
        if (existsDict != null && existsDict > 0) return;
        Object companyIdValue = body.get("companyId");
        Long companyId = companyIdValue == null ? user.getCompanyId() : ((Number) companyIdValue).longValue();
        Integer exists = jdbc.queryForObject(
                "select count(*) from biz_cert_type_request where cert_type=? and company_id <=> ? " +
                        "and status in ('pending','approved')", Integer.class, value, companyId);
        if (exists != null && exists > 0) return;
        jdbc.update("insert into biz_cert_type_request(company_id,cert_type,status,requested_by,create_time) " +
                "values(?,?,'pending',?,now())", companyId, value, user.getUserId());
    }

    public List<Map<String, Object>> requests(String status) {
        String where = status == null || status.isBlank() ? "" : " where r.status=? ";
        List<Object> args = new ArrayList<>();
        if (!where.isBlank()) args.add(status);
        return query(where, args);
    }

    public List<Map<String, Object>> myRequests(LoginUser user) {
        if (user.getCompanyId() == null) return List.of();
        return query(" where r.company_id=? ", List.of(user.getCompanyId()));
    }

    private List<Map<String, Object>> query(String where, List<Object> args) {
        return jdbc.queryForList(
                "select r.request_id as `requestId`, r.company_id as `companyId`, c.company_name as `companyName`, " +
                        "r.cert_type as `certType`, r.status, r.requested_by as `requestedBy`, " +
                        "u.nick_name as `requesterName`, r.review_comment as `reviewComment`, " +
                        "date_format(r.create_time,'%Y-%m-%d %H:%i') as `createTime`, " +
                        "date_format(r.review_time,'%Y-%m-%d %H:%i') as `reviewTime` " +
                        "from biz_cert_type_request r left join biz_company c on c.company_id=r.company_id " +
                        "left join sys_user u on u.user_id=r.requested_by" + where + " order by r.request_id desc", args.toArray());
    }

    @Transactional
    public void approve(Long requestId, String comment) {
        Map<String, Object> req = request(requestId);
        if (!"pending".equals(String.valueOf(req.get("status")))) {
            throw BizException.badRequest("该申请已处理");
        }
        String certType = String.valueOf(req.get("certType")).trim();
        Integer exists = jdbc.queryForObject(
                "select count(*) from sys_dict_data where dict_type='cert_type' and (dict_value=? or dict_label=?)",
                Integer.class, certType, certType);
        if (exists == null || exists == 0) {
            jdbc.update("insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status) " +
                            "values(99,?,?,'cert_type','', 'N','0')",
                    certType, certType);
        }
        jdbc.update("update biz_cert_type_request set status='approved',reviewed_by=?,review_comment=?,review_time=now() " +
                "where request_id=?", AuthContext.userId(), comment, requestId);
        notifyRequester(req, "证书类型审核通过",
                "证书类型「" + certType + "」已审核通过，已加入通用证书类型字典。");
    }

    @Transactional
    public void reject(Long requestId, String comment) {
        Map<String, Object> req = request(requestId);
        if (!"pending".equals(String.valueOf(req.get("status")))) {
            throw BizException.badRequest("该申请已处理");
        }
        jdbc.update("update biz_cert_type_request set status='rejected',reviewed_by=?,review_comment=?,review_time=now() " +
                "where request_id=?", AuthContext.userId(), comment, requestId);
        notifyRequester(req, "证书类型审核驳回",
                "证书类型「" + req.get("certType") + "」未通过审核" +
                        (comment == null || comment.isBlank() ? "。" : "，原因：" + comment));
    }

    @Transactional
    public void resubmit(Long requestId) {
        LoginUser user = AuthContext.require();
        Map<String, Object> req = request(requestId);
        Object company = req.get("companyId");
        if (user.getCompanyId() == null || company == null
                || !user.getCompanyId().equals(((Number) company).longValue())) {
            throw BizException.forbidden("无权操作其他公司的证书类型申请");
        }
        if (!"rejected".equals(String.valueOf(req.get("status")))) {
            throw BizException.badRequest("只有被驳回的申请可以重新提交");
        }
        jdbc.update("update biz_cert_type_request set status='pending',requested_by=?,reviewed_by=null," +
                        "review_comment=null,review_time=null,create_time=now() where request_id=?",
                user.getUserId(), requestId);
    }

    private void notifyRequester(Map<String, Object> req, String title, String content) {
        Object requester = req.get("requestedBy");
        if (requester == null) return;
        Object company = req.get("companyId");
        jdbc.update("insert into biz_message(msg_type,title,content,level,biz_type,biz_id,receiver_id,company_id,is_read,create_time) " +
                        "values('system',?,?,'1','cert_type_request',?,?,?, '0',now())",
                title, content, req.get("requestId"), ((Number) requester).longValue(), company);
    }

    private Map<String, Object> request(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select request_id as `requestId`, status, cert_type as `certType`, company_id as `companyId`, " +
                        "requested_by as `requestedBy` from biz_cert_type_request where request_id=?", id);
        if (rows.isEmpty()) throw BizException.badRequest("申请不存在");
        return rows.get(0);
    }
}
