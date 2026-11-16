package com.laminar.ops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laminar.ops.biz.entity.BizHospital;
import com.laminar.ops.biz.mapper.BizHospitalMapper;
import com.laminar.ops.common.BizException;
import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.DataScopeService;
import com.laminar.ops.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final BizHospitalMapper hospitalMapper;
    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;

    private static final List<String[]> REFERENCES = List.of(
            new String[]{"biz_campus", "hospital_id", "院区"},
            new String[]{"biz_department", "hospital_id", "科室"},
            new String[]{"biz_contact", "hospital_id", "对接人"},
            new String[]{"biz_equipment", "hospital_id", "设备"},
            new String[]{"biz_contract", "hospital_id", "合同"},
            new String[]{"biz_workorder", "hospital_id", "工单"},
            new String[]{"biz_hospital_user", "hospital_id", "院方账号"}
    );

    public IPage<BizHospital> page(int pageNum, int pageSize, BizHospital query) {
        LoginUser user = AuthContext.require();
        List<Long> hospitalIds = allowedHospitalIds(user);
        return hospitalMapper.selectPageWithRegion(new Page<>(pageNum, pageSize), query, hospitalIds);
    }

    public BizHospital detail(Long id) {
        checkScope(id);
        BizHospital hospital = hospitalMapper.selectDetail(id);
        if (hospital == null) {
            throw BizException.badRequest("医院不存在");
        }
        hospital.setCompanyIds(jdbc.queryForList(
                "select company_id from biz_company_hospital where hospital_id=? order by id", Long.class, id));
        return hospital;
    }

    public List<Map<String, Object>> managerOptions(Long id) {
        checkScope(id);
        return jdbc.queryForList(
                "select e.engineer_id as value, e.engineer_name as label " +
                        "from biz_engineer e join biz_hospital h on h.hospital_id=? " +
                        "join biz_company_hospital ch on ch.hospital_id=h.hospital_id and ch.company_id=e.company_id " +
                        "where e.region_id=h.region_id and e.status='0' " +
                        "order by e.engineer_name",
                id);
    }

    public void create(BizHospital hospital) {
        if (hospital.getRegionId() == null) throw BizException.badRequest("请选择所属区域");
        LoginUser user = AuthContext.require();
        checkRegionWrite(hospital.getRegionId());
        hospital.setHospitalId(null);
        normalizeCode(hospital);
        hospitalMapper.insert(hospital);
        syncRelations(hospital, user);
    }

    public void update(BizHospital hospital) {
        if (hospital.getRegionId() == null) throw BizException.badRequest("请选择所属区域");
        checkScope(hospital.getHospitalId());
        LoginUser user = AuthContext.require();
        checkRegionWrite(hospital.getRegionId());
        normalizeCode(hospital);
        hospitalMapper.updateById(hospital);
        syncRelations(hospital, user);
    }

    private void syncRelations(BizHospital hospital, LoginUser user) {
        Long hospitalId = hospital.getHospitalId();
        if (hospitalId == null) return;
        java.util.Set<Long> companyIds = new java.util.LinkedHashSet<>();
        if (hospital.getCompanyIds() != null) companyIds.addAll(hospital.getCompanyIds());
        Long ownCompany = dataScope.companyId(user);
        if (ownCompany != null) {
            companyIds.clear();
            companyIds.addAll(jdbc.queryForList(
                    "select company_id from biz_company_hospital where hospital_id=?", Long.class, hospitalId));
            companyIds.add(ownCompany);
        }
        if (companyIds.isEmpty()) return;
        List<Long> regions = jdbc.queryForList(
                "select region_id from biz_hospital where hospital_id=?", Long.class, hospitalId);
        Long regionId = regions.isEmpty() ? null : regions.get(0);
        jdbc.update("delete from biz_company_hospital where hospital_id=?", hospitalId);
        for (Long companyId : companyIds) {
            jdbc.update("insert ignore into biz_company_hospital(company_id,hospital_id,region_id,service_status,start_date) " +
                            "values(?,?,?,'0',curdate())",
                    companyId, hospitalId, regionId);
        }
    }

    private void checkRegionWrite(Long regionId) {
        LoginUser user = AuthContext.require();
        if ("region".equals(user.getDataScope())
                && (user.getRegionId() == null || !user.getRegionId().equals(regionId))) {
            throw BizException.forbidden("无权操作其他区域医院");
        }
    }

    private void normalizeCode(BizHospital hospital) {
        if (hospital.getHospitalCode() != null) {
            hospital.setHospitalCode(hospital.getHospitalCode().trim().toUpperCase());
        }
        if (hospital.getHospitalCode() == null || hospital.getHospitalCode().isBlank()) {
            throw BizException.badRequest("请填写医院编码，医院编码是医院主数据的唯一标识");
        }
    }

    public void remove(Long[] ids) {
        for (Long id : ids) {
            checkScope(id);
            checkReferences(id);
            jdbc.update("delete from biz_company_hospital where hospital_id=?", id);
            jdbc.update("delete from biz_company_campus where hospital_id=?", id);
            hospitalMapper.deleteById(id);
        }
    }

    private void checkReferences(Long hospitalId) {
        for (String[] ref : REFERENCES) {
            Integer count = jdbc.queryForObject("select count(*) from " + ref[0] + " where " + ref[1] + " = ?",
                    Integer.class, hospitalId);
            if (count != null && count > 0) {
                throw BizException.badRequest("存在关联的" + ref[2] + "（" + count + " 条），不能删除该医院");
            }
        }
    }

    public List<BizHospital> options() {
        LoginUser user = AuthContext.require();
        List<Long> ids = allowedHospitalIds(user);
        if (ids == null) return hospitalMapper.selectList(null);
        if (ids.isEmpty()) return List.of();
        return hospitalMapper.selectBatchIds(ids);
    }

    private void checkScope(Long id) {
        LoginUser user = AuthContext.require();
        if (!dataScope.canAccessHospital(user, id)) {
            throw BizException.forbidden("无权访问其他医院数据");
        }
        Long companyId = dataScope.companyId(user);
        if (companyId != null) {
            Integer count = jdbc.queryForObject(
                    "select count(*) from biz_company_hospital where hospital_id=? and company_id=?",
                    Integer.class, id, companyId);
            if (count == null || count == 0) {
                throw BizException.forbidden("无权访问其他公司医院");
            }
        }
    }

    private List<Long> allowedHospitalIds(LoginUser user) {
        List<Long> visible = dataScope.visibleHospitalIds(user);
        Long companyId = dataScope.companyId(user);
        if (companyId == null) return visible;
        List<Long> companyHospitals = jdbc.queryForList(
                "select hospital_id from biz_company_hospital where company_id=?", Long.class, companyId);
        if (visible == null) return companyHospitals;
        return visible.stream().filter(companyHospitals::contains).toList();
    }
}
