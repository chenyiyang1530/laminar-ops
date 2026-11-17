package com.laminar.ops.system.service;

import com.laminar.ops.common.SystemAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * 真实 MySQL 临时库的断点续跑测试，默认跳过。
 * 运行：MYSQL_IT=true MYSQL_IT_PASSWORD=... mvn test -Dtest=FileCheckMysqlResumeTest
 */
@EnabledIfEnvironmentVariable(named = "MYSQL_IT", matches = "true")
class FileCheckMysqlResumeTest {

    @Test
    void resumesRunningBatchFromMysqlCheckpoint() throws Exception {
        String host = env("MYSQL_IT_HOST", "127.0.0.1");
        String user = env("MYSQL_IT_USER", "root");
        String password = env("MYSQL_IT_PASSWORD", "root");
        String db = "laminar_filecheck_it_" + System.currentTimeMillis();
        String base = "jdbc:mysql://" + host + ":3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String url = "jdbc:mysql://" + host + ":3306/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        try (Connection conn = DriverManager.getConnection(base, user, password); Statement st = conn.createStatement()) {
            st.execute("create database `" + db + "`");
            st.execute("use `" + db + "`");
            st.execute("create table sys_file_check_run(run_id bigint primary key auto_increment, trigger_type varchar(20), status varchar(20), checked int default 0, missing int default 0, recovered int default 0, start_time datetime, end_time datetime, error_msg varchar(500))");
            st.execute("create table sys_file_check_item(item_id bigint primary key auto_increment, run_id bigint, table_name varchar(64), record_id bigint, file_column varchar(64), file_path varchar(500), file_type varchar(20), severity varchar(4), sla_hours int, sla_deadline datetime, company_id bigint, hospital_id bigint, status char(1) default '0', first_seen datetime, last_seen datetime, resolved_time datetime, escalated char(1) default '0', escalated_time datetime, escalation_level int default 0, last_escalation_time datetime)");
            st.execute("create table sys_file_check_checkpoint(run_id bigint, ref_key varchar(160), last_id bigint default 0, checked int default 0, missing int default 0, status varchar(20), updated_time datetime, primary key(run_id, ref_key))");
            st.execute("create table sys_file_check_lock(lock_name varchar(64) primary key, owner varchar(128), locked_at datetime)");
            st.execute("insert into sys_file_check_run(run_id,trigger_type,status,checked,missing,recovered,start_time) values(1,'scheduled','running',0,0,0,now())");
            for (FileCheckModels.FileRef ref : FileCheckModels.REFS) {
                st.execute("insert into sys_file_check_checkpoint(run_id,ref_key,last_id,checked,missing,status,updated_time) values(1,'" +
                        ref.table() + "|" + ref.column() + "',10,1,0,'done',now())");
            }
        }

        DriverManagerDataSource ds = new DriverManagerDataSource(url, user, password);
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        FileCheckScanService service = new FileCheckScanService(jdbc,
                mock(SystemAlertService.class), mock(FileCheckProperties.class),
                mock(FileCheckQueryService.class), mock(FileCheckNotificationService.class));
        ReflectionTestUtils.setField(service, "uploadPath", System.getProperty("java.io.tmpdir"));

        Map<String, Object> result = service.run("scheduled");
        assertEquals(1L, result.get("runId"));
        assertEquals("success", result.get("status"));
        assertEquals(9, result.get("checked"));
        assertEquals(0, result.get("missing"));

        try (Connection conn = DriverManager.getConnection(base, user, password); Statement st = conn.createStatement()) {
            st.execute("drop database `" + db + "`");
        }
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
