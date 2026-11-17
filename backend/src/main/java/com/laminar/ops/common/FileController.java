package com.laminar.ops.common;

import com.laminar.ops.security.AuthContext;
import com.laminar.ops.security.LoginUser;
import com.laminar.ops.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class FileController {

    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "webp", "pdf", "doc", "docx", "xls", "xlsx");
    private static final Map<String, String> MIME = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp",
            "pdf", "application/pdf", "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SysConfigService sysConfig;
    private final FileSignService fileSignService;
    private final UploadScanService uploadScanService;
    private final JdbcTemplate jdbc;
    private final Tika tika = new Tika();

    @Value("${laminar.upload.path:./upload}")
    private String uploadPath;

    @GetMapping("/file/sign")
    public Result<Map<String, Object>> sign(@RequestParam String path) {
        LoginUser user = AuthContext.require();
        String relative = normalize(path);
        Path target = resolve(relative);
        checkAccess(user, relative, target);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", fileSignService.signedUrl(relative));
        result.put("expiresIn", fileSignService.ttlSeconds());
        return Result.ok(result);
    }

    @GetMapping("/signed-file")
    public ResponseEntity<Resource> signedDownload(@RequestParam String path,
                                                   @RequestParam Long expires,
                                                   @RequestParam String signature) {
        String relative = normalize(path);
        if (!fileSignService.verify(relative, expires, signature)) {
            throw new BizException(403, "文件下载链接无效或已过期");
        }
        Path target = resolve(relative);
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return serve(target);
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> download(@RequestParam String path) {
        LoginUser user = AuthContext.require();
        String relative = normalize(path);
        Path target = resolve(relative);
        checkAccess(user, relative, target);
        return serve(target);
    }

    private ResponseEntity<Resource> serve(Path target) {
        FileSystemResource resource = new FileSystemResource(target);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        String filename = target.getFileName().toString().replaceAll("[\\r\\n\"]", "_");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofMinutes(5)).cachePrivate())
                .body(resource);
    }

    private Path resolve(String relative) {
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new BizException(400, "非法文件路径");
        return target;
    }

    private void checkAccess(LoginUser user, String relative, Path target) {
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        if (user.isHospitalUser() && relative.startsWith("company/")) {
            String legacyPath = "/upload/" + relative;
            Integer linked = jdbc.queryForObject(
                    "select count(*) from biz_workorder where hospital_id=? and " +
                            "(finish_image in (?,?) or acceptance_image in (?,?))",
                    Integer.class, user.getHospitalId(), relative, legacyPath, relative, legacyPath);
            if (linked != null && linked > 0) return;
        }
        if (!user.isSuperAdmin() && relative.startsWith("company/")) {
            Long companyId = user.getCompanyId();
            if (companyId == null || !relative.startsWith("company/" + companyId + "/")) {
                throw new BizException(403, "无权访问其他公司的文件");
            }
        }
    }

    private String normalize(String path) {
        String relative = path == null ? "" : path.replace('\\', '/');
        while (relative.startsWith("/")) relative = relative.substring(1);
        return relative;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(defaultValue = "common") String category) {
        if (file == null || file.isEmpty()) throw new BizException(400, "请选择要上传的文件");
        int maxMb = Math.max(1, sysConfig.intValue("platform.upload.maxSizeMb", 20));
        long maxSize = maxMb * 1024L * 1024L;
        if (file.getSize() > maxSize) throw new BizException(400, "文件不能超过 " + maxMb + "MB");
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        Set<String> allowed = allowedExtensions();
        if (!allowed.contains(ext)) {
            throw new BizException(400, "不支持的文件类型，仅支持：" + String.join("、", allowed));
        }
        String detected;
        try (java.io.InputStream in = file.getInputStream()) {
            detected = tika.detect(in, original);
        } catch (IOException e) {
            throw new BizException(400, "无法识别文件内容");
        }
        if (!matchesExtension(ext, detected)) {
            throw new BizException(400, "文件内容与扩展名不匹配，已拦截");
        }
        uploadScanService.scan(file);
        String safeCategory = category == null ? "common" : category.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeCategory.isBlank()) safeCategory = "common";
        LoginUser user = AuthContext.require();
        String owner = user.getCompanyId() == null ? "platform" : "company/" + user.getCompanyId();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String relative = owner + "/" + safeCategory + "/" + date + "/" + UUID.randomUUID() + "." + ext;

        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new BizException(400, "非法文件路径");
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BizException(500, "文件保存失败");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", "/upload/" + relative);
        result.put("path", relative);
        result.put("name", original);
        result.put("size", file.getSize());
        result.put("type", MIME.getOrDefault(ext, "application/octet-stream"));
        return Result.ok(result);
    }

    private boolean matchesExtension(String ext, String detected) {
        if (detected == null) return false;
        String type = detected.toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpg", "jpeg" -> type.equals("image/jpeg");
            case "png" -> type.equals("image/png");
            case "webp" -> type.equals("image/webp");
            case "pdf" -> type.equals("application/pdf");
            case "doc" -> type.equals("application/msword") || type.equals("application/x-tika-msoffice");
            case "xls" -> type.equals("application/vnd.ms-excel") || type.equals("application/x-tika-msoffice");
            case "docx" -> type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "xlsx" -> type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> false;
        };
    }

    private Set<String> allowedExtensions() {
        String configured = sysConfig.value("platform.upload.allowedTypes", "");
        Set<String> result = new java.util.LinkedHashSet<>();
        for (String item : configured.split(",")) {
            String ext = item.trim().toLowerCase(Locale.ROOT).replace(".", "");
            if (!ext.isBlank()) result.add(ext);
        }
        return result.isEmpty() ? ALLOWED_EXT : result;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
