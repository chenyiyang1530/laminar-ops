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

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final BizHospitalMapper hospitalMapper;
    private final JdbcTemplate jdbc;
    private final DataScopeService dataScope;
    private final ContractScopeService contractScope;

    private static final List<String[]> REFERENCES = List.of(
            new String[]{"biz_campus", "hospital_id", "院区"},
            new String[]{"biz_department", "hospital_id", "科室"},
            new String[]{"biz_clean_area", "hospital_id", "洁净区域"},
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
        return hospital;
    }

    public void create(BizHospital hospital) {
        checkCustomerWritable();
        if (hospital.getRegionId() == null) throw BizException.badRequest("请选择所属区域");
        checkRegionWrite(hospital.getRegionId());
        hospital.setHospitalId(null);
        normalizeCode(hospital);
        hospitalMapper.insert(hospital);
    }

    public void update(BizHospital hospital) {
        checkCustomerWritable();
        if (hospital.getRegionId() == null) throw BizException.badRequest("请选择所属区域");
        checkScope(hospital.getHospitalId());
        checkRegionWrite(hospital.getRegionId());
        normalizeCode(hospital);
        hospitalMapper.updateById(hospital);
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
    }

    public void remove(Long[] ids) {
        checkCustomerWritable();
        for (Long id : ids) {
            checkScope(id);
            checkReferences(id);
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
        if (companyId != null && !contractScope.hospitalIdsForCompany(companyId).contains(id)) {
            throw BizException.forbidden("无权访问其他公司客户数据");
        }
    }

    private void checkCustomerWritable() {
        if (dataScope.companyId(AuthContext.require()) != null) {
            throw BizException.forbidden("客户主数据由平台管理员维护");
        }
    }

    private List<Long> allowedHospitalIds(LoginUser user) {
        List<Long> visible = dataScope.visibleHospitalIds(user);
        Long companyId = dataScope.companyId(user);
        if (companyId == null) return visible;
        List<Long> companyHospitals = contractScope.hospitalIdsForCompany(companyId);
        if (visible == null) return companyHospitals;
        return visible.stream().filter(companyHospitals::contains).toList();
    }
}
