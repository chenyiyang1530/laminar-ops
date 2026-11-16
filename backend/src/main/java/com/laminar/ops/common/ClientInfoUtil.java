package com.laminar.ops.common;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 登录会话展示信息：登录地点、浏览器、操作系统。
 * 内网/回环直接标记；公网 IP 使用带缓存的 IP 归属地查询，查询失败时回退为 IP。
 */
public final class ClientInfoUtil {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(900))
            .build();
    private static final Map<String, CachedLocation> LOCATION_CACHE = new ConcurrentHashMap<>();
    private static final long LOCATION_TTL_MS = 6 * 3600_000L;

    private ClientInfoUtil() {
    }

    public static String location(String ip) {
        if (ip == null || ip.isBlank()) return "未知";
        String value = ip.trim();
        if ("127.0.0.1".equals(value) || "::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value)) {
            return "本机";
        }
        if ("localhost".equalsIgnoreCase(value)) return "本机";
        if (isPrivate(value)) return "内网";
        CachedLocation cached = LOCATION_CACHE.get(value);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt() > now) return cached.value();
        String resolved = lookup(value);
        LOCATION_CACHE.put(value, new CachedLocation(resolved, now + LOCATION_TTL_MS));
        return resolved;
    }

    private static String lookup(String ip) {
        try {
            String url = "http://ip-api.com/json/" + URLEncoder.encode(ip, StandardCharsets.UTF_8)
                    + "?lang=zh-CN&fields=status,country,regionName,city";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(1200))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body();
            if (body == null || !json(body, "status").equals("success")) return "IP " + ip;
            List<String> parts = new ArrayList<>();
            add(parts, json(body, "country"));
            add(parts, json(body, "regionName"));
            add(parts, json(body, "city"));
            return parts.isEmpty() ? "IP " + ip : String.join(" ", parts);
        } catch (Exception e) {
            return "IP " + ip;
        }
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank() && !parts.contains(value)) parts.add(value);
    }

    private static String json(String body, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    private record CachedLocation(String value, long expireAt) {
    }

    private static boolean isPrivate(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isSiteLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    public static String browser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "未知";
        String ua = userAgent;
        if (ua.contains("Edg/")) return "Edge " + version(ua, "Edg/");
        if (ua.contains("OPR/")) return "Opera " + version(ua, "OPR/");
        if (ua.contains("Firefox/")) return "Firefox " + version(ua, "Firefox/");
        if (ua.contains("Chrome/")) return "Chrome " + version(ua, "Chrome/");
        if (ua.contains("Safari/") && ua.contains("Version/")) return "Safari " + version(ua, "Version/");
        if (ua.contains("MicroMessenger/")) return "微信内置浏览器";
        return "其他";
    }

    public static String os(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "未知";
        String ua = userAgent;
        if (ua.contains("Windows NT 10.0")) return "Windows 10/11";
        if (ua.contains("Windows NT 6.3")) return "Windows 8.1";
        if (ua.contains("Windows NT 6.1")) return "Windows 7";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "其他";
    }

    private static String version(String ua, String marker) {
        int start = ua.indexOf(marker);
        if (start < 0) return "";
        int from = start + marker.length();
        int end = from;
        while (end < ua.length() && (Character.isDigit(ua.charAt(end)) || ua.charAt(end) == '.')) end++;
        return ua.substring(from, end);
    }
}
