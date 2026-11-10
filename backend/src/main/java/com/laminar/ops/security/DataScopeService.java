package com.laminar.ops.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一数据范围：all / region / hospital / self。
 * visibleHospitalIds 返回 null 表示不限制（全部）。
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final JdbcTemplate jdbc;

    public List<Long> visibleHospitalIds(LoginUser user) {
        if (user == null) return List.of();
        if (user.isHospitalUser()) {
            return user.getHospitalId() == null ? List.of() : List.of(user.getHospitalId());
        }
        if (user.isSuperAdmin() || "all".equals(user.getDataScope())) {
            return null;
        }
        if ("region".equals(user.getDataScope())) {
            if (user.getRegionId() == null) return List.of();
            List<Long> regions = visibleRegionIds(user);
            if (regions == null || regions.isEmpty()) return List.of();
            return jdbc.queryForList(
                    "select hospital_id from biz_hospital where region_id in (" +
                            regions.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) +
                            ") and status='0'",
                    Long.class);
        }
        if ("hospital".equals(user.getDataScope())) {
            return user.getHospitalId() == null ? List.of() : List.of(user.getHospitalId());
        }
        // self：按工程师服务的医院
        List<Long> ids = new ArrayList<>();
        if (user.getEngineerId() != null) {
            ids = jdbc.queryForList(
                    "select hospital_id from biz_engineer_hospital where engineer_id=?",
                    Long.class, user.getEngineerId());
        }
        if (ids.isEmpty() && user.getHospitalId() != null) {
            ids.add(user.getHospitalId());
        }
        return ids;
    }

    public boolean canAccessHospital(LoginUser user, Long hospitalId) {
        List<Long> ids = visibleHospitalIds(user);
        return ids == null || (hospitalId != null && ids.contains(hospitalId));
    }

    public Long companyId(LoginUser user) {
        return user == null || user.isSuperAdmin() ? null : user.getCompanyId();
    }

    /** 区域范围：本区域及其全部下级区域；all 返回 null 表示不限。 */
    public List<Long> visibleRegionIds(LoginUser user) {
        if (user == null) return List.of();
        if (user.isSuperAdmin() || "all".equals(user.getDataScope())) return null;
        if (!"region".equals(user.getDataScope()) || user.getRegionId() == null) {
            return user.getRegionId() == null ? List.of() : List.of(user.getRegionId());
        }
        List<Long> result = new ArrayList<>();
        result.add(user.getRegionId());
        List<Long> current = List.of(user.getRegionId());
        int guard = 0;
        while (!current.isEmpty() && guard++ < 20) {
            StringBuilder in = new StringBuilder();
            for (Long id : current) {
                if (in.length() > 0) in.append(",");
                in.append(id);
            }
            current = jdbc.queryForList(
                    "select region_id from biz_region where parent_id in (" + in + ") and status='0'", Long.class);
            for (Long id : current) if (!result.contains(id)) result.add(id);
        }
        return result;
    }

    public boolean canAccessRegion(LoginUser user, Long regionId) {
        List<Long> ids = visibleRegionIds(user);
        return ids == null || (regionId != null && ids.contains(regionId));
    }

    public boolean canAccessEquipment(LoginUser user, Long equipmentId) {
        List<Long> hospitals = jdbc.queryForList(
                "select hospital_id from biz_equipment where equipment_id=?", Long.class, equipmentId);
        return !hospitals.isEmpty() && canAccessHospital(user, hospitals.get(0));
    }

    public boolean canAccessPoint(LoginUser user, Long pointId) {
        List<Long> hospitals = jdbc.queryForList(
                "select e.hospital_id from biz_monitor_point p join biz_equipment e on e.equipment_id=p.equipment_id " +
                        "where p.point_id=?", Long.class, pointId);
        return !hospitals.isEmpty() && canAccessHospital(user, hospitals.get(0));
    }

    /** 院方/单医院范围时返回唯一医院，否则 null */
    public Long singleHospital(LoginUser user) {
        List<Long> ids = visibleHospitalIds(user);
        return ids != null && ids.size() == 1 ? ids.get(0) : null;
    }
}
