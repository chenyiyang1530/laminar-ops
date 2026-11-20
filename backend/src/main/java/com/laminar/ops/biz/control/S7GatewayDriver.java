package com.laminar.ops.biz.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * S7 边缘网关驱动。
 * 网关需提供：
 *   GET  /api/plc/read?address=100&unit=1
 *   POST /api/plc/write  {"address":100,"value":1,"unit":1}
 * 读取响应：{"value":1}；写入响应：{"ok":true}
 */
public class S7GatewayDriver implements ControlDriver {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    @Override
    public String protocol() {
        return "S7_GATEWAY";
    }

    @Override
    public int read(DeviceControlConfig config, int address) throws Exception {
        String url = base(config) + "/api/plc/read?address=" + address + "&unit=" + config.unitId();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(config.timeoutMs())).GET().build();
        JsonNode node = send(request);
        if (!node.path("ok").asBoolean(true)) throw new IllegalStateException(node.path("error").asText("S7 网关读取失败"));
        return node.path("value").asInt();
    }

    @Override
    public void write(DeviceControlConfig config, int address, int value) throws Exception {
        String body = MAPPER.writeValueAsString(Map.of("address", address, "value", value, "unit", config.unitId()));
        HttpRequest request = HttpRequest.newBuilder(URI.create(base(config) + "/api/plc/write"))
                .timeout(Duration.ofMillis(config.timeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        JsonNode node = send(request);
        if (!node.path("ok").asBoolean(false)) throw new IllegalStateException(node.path("error").asText("S7 网关写入失败"));
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("S7 网关 HTTP " + response.statusCode());
        }
        return MAPPER.readTree(response.body());
    }

    private String base(DeviceControlConfig config) {
        if (config.host() == null || config.host().isBlank()) throw new IllegalStateException("未配置 S7 网关地址");
        String host = config.host().trim();
        if (host.startsWith("http://") || host.startsWith("https://")) return host.replaceAll("/$", "");
        return "http://" + host + ":" + config.port();
    }
}
