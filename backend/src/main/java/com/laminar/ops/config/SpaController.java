package com.laminar.ops.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端单页应用路由回退：/app/** 下非静态资源请求统一返回 index.html。
 */
@Controller
public class SpaController {

    @GetMapping("/")
    public String root() {
        return "redirect:/app/";
    }

    @GetMapping({"/app", "/app/", "/app/{p1:[^\\.]*}", "/app/{p1:[^\\.]*}/{p2:[^\\.]*}"})
    public String index() {
        return "forward:/app/index.html";
    }

    @GetMapping({"/portal", "/portal/", "/portal/{p1:[^\\.]*}", "/portal/{p1:[^\\.]*}/{p2:[^\\.]*}"})
    public String portal() {
        return "forward:/portal/index.html";
    }
}
