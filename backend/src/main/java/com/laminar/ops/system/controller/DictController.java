package com.laminar.ops.system.controller;

import com.laminar.ops.common.Result;
import com.laminar.ops.system.entity.SysDictData;
import com.laminar.ops.system.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/type/{dictType}")
    public Result<List<SysDictData>> byType(@PathVariable String dictType) {
        return Result.ok(dictService.listByType(dictType));
    }
}
