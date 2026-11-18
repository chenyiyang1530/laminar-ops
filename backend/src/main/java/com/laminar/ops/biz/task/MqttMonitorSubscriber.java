package com.laminar.ops.biz.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laminar.ops.biz.service.MonitorService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 可选 MQTT 原生接入：订阅现场 Topic，把 JSON 直接送入设备接入通道。
 * 默认关闭，设置 MQTT_ENABLED=true 后启用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "laminar.monitor.mqtt", name = "enabled", havingValue = "true")
public class MqttMonitorSubscriber implements MqttCallbackExtended {

    private final MonitorService monitorService;
    private final ObjectMapper objectMapper;

    @Value("${laminar.monitor.mqtt.broker:tcp://127.0.0.1:1883}")
    private String broker;
    @Value("${laminar.monitor.mqtt.client-id:}")
    private String clientId;
    @Value("${laminar.monitor.mqtt.topic:laminar/monitor/up}")
    private String topic;
    @Value("${laminar.monitor.mqtt.username:}")
    private String username;
    @Value("${laminar.monitor.mqtt.password:}")
    private String password;
    @Value("${laminar.monitor.mqtt.qos:1}")
    private int qos;

    private MqttClient client;

    @PostConstruct
    public void start() {
        try {
            String id = clientId == null || clientId.isBlank()
                    ? "laminar-ops-" + UUID.randomUUID().toString().substring(0, 8)
                    : clientId;
            client = new MqttClient(broker, id, new MemoryPersistence());
            client.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            if (username != null && !username.isBlank()) options.setUserName(username);
            if (password != null && !password.isBlank()) options.setPassword(password.toCharArray());
            client.connect(options);
            client.subscribe(topic, qos);
            log.info("[MQTT] 已连接 {}，订阅 {}", broker, topic);
        } catch (Exception e) {
            log.warn("[MQTT] 连接失败，设备接入 HTTP 通道不受影响: {}", e.getMessage());
            closeQuietly();
        }
    }

    @PreDestroy
    public void stop() {
        closeQuietly();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.subscribe(topic, qos);
            log.info("[MQTT] {}，重新订阅 {}", reconnect ? "重连成功" : "连接成功", topic);
        } catch (Exception e) {
            log.warn("[MQTT] 订阅失败: {}", e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("[MQTT] 连接断开，自动重连中: {}", cause == null ? "" : cause.getMessage());
    }

    @Override
    public void messageArrived(String incomingTopic, MqttMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            if (root.isArray()) {
                root.forEach(this::ingest);
            } else if (root.has("points") && root.get("points").isArray()) {
                root.get("points").forEach(this::ingest);
            } else {
                ingest(root);
            }
        } catch (Exception e) {
            log.warn("[MQTT] 消息解析失败 topic={}: {}", incomingTopic, e.getMessage());
        }
    }

    private void ingest(JsonNode node) {
        String equipmentCode = node.path("equipmentCode").asText("");
        String pointCode = node.path("pointCode").asText("");
        if (equipmentCode.isBlank() || pointCode.isBlank() || !node.has("value")) return;
        try {
            double value = node.get("value").asDouble();
            LocalDateTime timestamp = timestamp(node.get("timestamp"));
            monitorService.report(equipmentCode, pointCode, value, timestamp);
        } catch (Exception e) {
            log.warn("[MQTT] 点位写入失败 {}/{}: {}", equipmentCode, pointCode, e.getMessage());
        }
    }

    private LocalDateTime timestamp(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isBlank()) return LocalDateTime.now();
        String text = node.asText().trim();
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
                return LocalDateTime.now();
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // no-op
    }

    private void closeQuietly() {
        try {
            if (client != null && client.isConnected()) client.disconnect();
            if (client != null) client.close();
        } catch (Exception ignored) {
            // ignore
        }
    }
}
