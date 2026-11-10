package com.laminar.ops.biz.controller;

import com.laminar.ops.biz.service.MessageService;
import com.laminar.ops.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String msgType,
                                            @RequestParam(required = false) String isRead) {
        return Result.ok(messageService.page(pageNum, pageSize, msgType, isRead));
    }

    @GetMapping("/unread-count")
    public Result<Integer> unread() {
        return Result.ok(messageService.unreadCount());
    }

    @PutMapping("/read/{id}")
    public Result<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        messageService.readAll();
        return Result.ok();
    }
}
