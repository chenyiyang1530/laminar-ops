package com.laminar.ops;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 层流净化设备智能监测运维一体化平台
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.laminar.ops.**.mapper")
public class LaminarOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaminarOpsApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        com.laminar.ops.system.service.VersionService version =
                event.getApplicationContext().getBean(com.laminar.ops.system.service.VersionService.class);
        log.info("==========================================================");
        log.info("  层流净化运维平台启动成功！");
        log.info("  版本: {}  构建时间: {}", version.info().get("version"), version.info().get("buildTime"));
        log.info("  接口地址: http://localhost:{}{}", port, contextPath);
        log.info("  版本检查: http://localhost:{}{}api/public/version", port, contextPath);
        log.info("  健康检查: http://localhost:{}{}actuator/health", port, contextPath.startsWith("/") ? contextPath : "/" + contextPath);
        log.info("==========================================================");
    }
}

