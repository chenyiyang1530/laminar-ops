package com.laminar.ops.common.crud;

import com.laminar.ops.common.Result;
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
import java.util.Map;

@RestController
@RequestMapping("/api/crud/{res}")
@RequiredArgsConstructor
public class CrudController {

    private final GenericCrudService crudService;

    @GetMapping("/meta")
    public Result<Map<String, Object>> meta(@PathVariable String res) {
        return Result.ok(crudService.meta(res));
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@PathVariable String res, @RequestParam Map<String, String> params) {
        return Result.ok(crudService.page(res, params));
    }

    @GetMapping("/options")
    public Result<List<Map<String, Object>>> options(@PathVariable String res,
                                                     @RequestParam Map<String, String> params) {
        return Result.ok(crudService.options(res, params));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable String res, @PathVariable Long id) {
        return Result.ok(crudService.get(res, id));
    }

    @PostMapping
    public Result<Long> save(@PathVariable String res, @RequestBody Map<String, Object> body) {
        return Result.ok(crudService.save(res, body));
    }

    @PutMapping
    public Result<Void> update(@PathVariable String res, @RequestBody Map<String, Object> body) {
        crudService.update(res, body);
        return Result.ok();
    }

    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String res, @PathVariable Long[] ids) {
        crudService.delete(res, ids);
        return Result.ok();
    }
}
