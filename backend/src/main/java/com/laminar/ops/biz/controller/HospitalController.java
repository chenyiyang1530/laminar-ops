package com.laminar.ops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laminar.ops.biz.entity.BizHospital;
import com.laminar.ops.biz.service.HospitalService;
import com.laminar.ops.common.PageResult;
import com.laminar.ops.common.Result;
import com.laminar.ops.security.Perm;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/hospital")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping("/page")
    @Perm("customer:hospital:list")
    public Result<PageResult<BizHospital>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                BizHospital query) {
        IPage<BizHospital> page = hospitalService.page(pageNum, pageSize, query);
        return Result.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/options")
    public Result<List<BizHospital>> options() {
        return Result.ok(hospitalService.options());
    }

    @GetMapping("/{id}")
    @Perm("customer:hospital:list")
    public Result<BizHospital> detail(@PathVariable Long id) {
        return Result.ok(hospitalService.detail(id));
    }

    @PostMapping
    @Perm("customer:hospital:add")
    public Result<Void> add(@RequestBody BizHospital hospital) {
        hospitalService.create(hospital);
        return Result.ok();
    }

    @PutMapping
    @Perm("customer:hospital:edit")
    public Result<Void> edit(@RequestBody BizHospital hospital) {
        hospitalService.update(hospital);
        return Result.ok();
    }

    @DeleteMapping("/{ids}")
    @Perm("customer:hospital:remove")
    public Result<Void> remove(@PathVariable Long[] ids) {
        hospitalService.remove(ids);
        return Result.ok();
    }
}
