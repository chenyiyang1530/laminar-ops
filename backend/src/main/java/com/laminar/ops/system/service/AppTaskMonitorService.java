package com.laminar.ops.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 读取 Windows 计划任务 LaminarOpsBackend 状态和启动日志。 */
@Slf4j
@Service
public class AppTaskMonitorService {

    @Value("${laminar.autostart.task-name:LaminarOpsBackend}")
    private String taskName;

    @Value("${laminar.autostart.log-dir:D:/ideaWork/laminar-ops/logs}")
    private String logDir;

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskName", taskName);
        try {
            String script = "$t=Get-ScheduledTask -TaskName '" + taskName + "' -ErrorAction Stop; " +
                    "$i=Get-ScheduledTaskInfo -TaskName '" + taskName + "' -ErrorAction Stop; " +
                    "[pscustomobject]@{state=[string]$t.State; lastRunTime=[string]$i.LastRunTime; " +
                    "lastTaskResult=$i.LastTaskResult; nextRunTime=[string]$i.NextRunTime; " +
                    "numberOfMissedRuns=$i.NumberOfMissedRuns; taskPath=[string]$t.TaskPath}|ConvertTo-Json -Compress";
            String json = execute("powershell.exe", "-NoProfile", "-Command", script);
            JsonNode node = new ObjectMapper().readTree(json);
            result.put("state", text(node, "state"));
            result.put("lastRunTime", text(node, "lastRunTime"));
            result.put("nextRunTime", text(node, "nextRunTime"));
            result.put("lastTaskResult", node.path("lastTaskResult").asLong());
            result.put("numberOfMissedRuns", node.path("numberOfMissedRuns").asInt());
            result.put("failureReason", failureReason(node.path("lastTaskResult").asLong()));
            result.put("ok", true);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("state", "UNKNOWN");
            result.put("failureReason", "无法读取计划任务（可能未安装、当前进程无权限或非 SYSTEM）");
            result.put("detail", e.getMessage());
        }
        result.put("logs", logFiles());
        return result;
    }

    public Map<String, Object> log(String type, int lines) {
        String file = "err".equals(type) ? "autostart-app.err" : "autostart-app.log";
        Path path = Path.of(logDir).resolve(file);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("file", path.toString());
        try {
            if (!Files.exists(path)) {
                result.put("content", "日志文件不存在");
                return result;
            }
            List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
            int from = Math.max(0, all.size() - Math.min(Math.max(lines, 20), 500));
            result.put("content", String.join("\n", all.subList(from, all.size())));
        } catch (Exception e) {
            result.put("content", "读取日志失败：" + e.getMessage());
        }
        return result;
    }

    private List<Map<String, Object>> logFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        Path dir = Path.of(logDir);
        for (String name : List.of("autostart-app.log", "autostart-app.err")) {
            Path path = dir.resolve(name);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("path", path.toString());
            row.put("exists", Files.exists(path));
            row.put("size", Files.exists(path) ? path.toFile().length() : 0);
            files.add(row);
        }
        return files;
    }

    private String failureReason(long code) {
        if (code == 0L) return "上次运行成功";
        if (code == 267009L) return "任务正在运行";
        if (code == 267011L) return "任务尚未运行";
        if (code == 267014L) return "任务被终止";
        if (code == 2147942402L) return "程序不存在，请检查启动脚本路径";
        if (code == 2147942403L) return "路径不存在，请检查工作目录";
        if (code == 1L) return "启动脚本执行失败，请查看启动日志";
        return "任务返回码 " + code + "，请查看启动日志";
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private String execute(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line);
        }
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new RuntimeException("读取计划任务超时");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("PowerShell 退出码 " + process.exitValue() + "：" + output);
        }
        return output.toString();
    }
}
