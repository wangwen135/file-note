package com.wwh.filenote.controller;

import com.wwh.filenote.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件笔记控制器
 * 提供文件上传、下载、删除和查询等功能的API接口
 */
@Controller
public class FileNoteController {

    private static final Logger logger = LoggerFactory.getLogger(FileNoteController.class);


    @PostMapping("/upload_file")
    @ResponseBody
    public ResponseEntity<String> uploadFile(HttpSession session, @RequestParam("file") MultipartFile[] files) throws IOException {
        String username = (String) session.getAttribute("username");
        if (username == null) username = "unknown";
        logger.info("用户 {} 尝试上传 {} 个文件", username, files.length);


        if (files == null || files.length == 0) {
            logger.warn("用户 {} 上传失败: 未选择文件", username);
            return ResponseEntity.badRequest().body("未选择文件");
        }

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("用户 {} 上传失败: 存储目录未找到", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("存储目录未找到");
        }

        Path uploadDir = Paths.get(storageDir, new SimpleDateFormat("yyyy").format(new Date()), new SimpleDateFormat("MM").format(new Date()));
        Files.createDirectories(uploadDir);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());

        for (MultipartFile f : files) {
            String filename = FileUtils.safeUnicodeFilename(f.getOriginalFilename() != null ? f.getOriginalFilename() : "pasted_file");
            String savedFilename = filename.substring(0, filename.lastIndexOf('.')) + "_" + timestamp + filename.substring(filename.lastIndexOf('.'));
            Path savePath = uploadDir.resolve(savedFilename);
            Files.copy(f.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("用户 {} 上传文件: {}", username, savedFilename);
        }

        return ResponseEntity.ok("OK");
    }

    @PostMapping("/upload_text")
    @ResponseBody
    public ResponseEntity<String> uploadText(HttpSession session, @RequestBody Map<String, Object> body) throws IOException {
        String username = (String) session.getAttribute("username");
        if (username == null) username = "unknown";
        logger.info("用户 {} 尝试上传文本", username);


        if (body == null || !body.containsKey("text") || ((String) body.get("text")).trim().isEmpty()) {
            logger.warn("用户 {} 文本上传失败: 内容为空", username);
            return ResponseEntity.badRequest().body("无文本内容");
        }

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("用户 {} 文本上传失败: 存储目录未找到", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("存储目录未找到");
        }

        Path uploadDir = Paths.get(storageDir, new SimpleDateFormat("yyyy").format(new Date()), new SimpleDateFormat("MM").format(new Date()));
        Files.createDirectories(uploadDir);
        String filename = "paste_" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date()) + ".txt";
        Path savePath = uploadDir.resolve(filename);
        Files.write(savePath, ((String) body.get("text")).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        logger.info("用户 {} 上传文本文件: {}", username, filename);

        return ResponseEntity.ok("OK");
    }

    @DeleteMapping("/delete_file")
    @ResponseBody
    public ResponseEntity<String> deleteFile(@RequestParam("path") String filePath, HttpSession session) throws IOException {
        String username = (String) session.getAttribute("username");
        if (username == null) username = "unknown";
        logger.info("用户 {} 尝试删除文件: {}", username, filePath);

        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role)) {
            logger.warn("用户 {} 未授权删除尝试: {}", username, filePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无删除权限");
        }

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("用户 {} 删除失败: 存储目录未找到", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("存储目录未找到");
        }

        // 确保文件路径以'/'开头的情况下也能正确处理
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        // 确保路径是存储路径 + 请求参数中的路径
        Path basePath = Paths.get(storageDir).toAbsolutePath().normalize();
        // 使用Paths.get()直接拼接，确保路径正确性
        Path targetPath = Paths.get(basePath.toString(), filePath).toAbsolutePath().normalize();

        // 安全检查：确保目标路径在存储目录内
        if (!targetPath.startsWith(basePath)) {
            logger.warn("用户 {} 尝试路径遍历: {}", username, filePath);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("无效的文件路径");
        }

        if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
            Files.delete(targetPath);
            logger.info("用户 {} 成功删除文件: {}", username, filePath);
            return ResponseEntity.ok("文件删除成功");
        } else {
            logger.warn("用户 {} 删除失败: 文件未找到 - {}", username, filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在");
        }
    }

    @GetMapping("/list_periods")
    @ResponseBody
    public ResponseEntity<?> listPeriods(HttpSession session) throws IOException {

        String storageDir = (String) session.getAttribute("storageDir");


        File root = new File(storageDir);
        List<Map<String, Object>> periods = new ArrayList<>();
        if (root.exists() && root.isDirectory()) {
            File[] years = root.listFiles(File::isDirectory);
            if (years != null) {
                for (File ydir : years) {
                    String yname = ydir.getName();
                    if (yname.matches("\\d+")) {
                        File[] months = ydir.listFiles(File::isDirectory);
                        List<String> mlist = new ArrayList<>();
                        if (months != null) {
                            for (File mdir : months) {
                                String mname = mdir.getName();
                                if (mname.matches("\\d+")) {
                                    mlist.add(mname);
                                }
                            }
                        }
                        // 按数字降序排序
                        mlist.sort((a, b) -> Integer.compare(Integer.parseInt(b), Integer.parseInt(a)));
                        Map<String, Object> map = new HashMap<>();
                        map.put("year", yname);
                        map.put("months", mlist);
                        periods.add(map);
                    }
                }
            }
        }
        periods.sort((a, b) -> Integer.parseInt((String) b.get("year")) - Integer.parseInt((String) a.get("year")));
        Map<String, Object> resp = new HashMap<>();
        resp.put("periods", periods);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/list_files")
    @ResponseBody
    public ResponseEntity<?> listFiles(HttpSession session,
                                       @RequestParam(value = "year", required = false) String year,
                                       @RequestParam(value = "month", required = false) String month,
                                       @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) throws IOException {

        String storageDir = (String) session.getAttribute("storageDir");
        Path root = Paths.get(storageDir);

        List<Map<String, Object>> result = new ArrayList<>();
        if (year != null && month != null && !year.isEmpty() && !month.isEmpty()) {
            Path folder = root.resolve(year).resolve(month);
            if (Files.exists(folder) && Files.isDirectory(folder)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
                    for (Path p : ds) {
                        if (Files.isRegularFile(p)) {
                            result.add(buildRecord(p, year, month));
                        }
                    }
                }
                // 按时间降序排序
                result.sort((a, b) -> ((String) b.get("time")).compareTo((String) a.get("time")));
            }
        } else {
            // 遍历并收集文件信息
            try {
                List<Map<String, Object>> all = new ArrayList<>();
                Files.walk(root)
                        .filter(Files::isRegularFile)
                        .forEach(p -> {
                            Path rel = root.relativize(p);
                            if (rel.getNameCount() >= 3) {
                                String y = rel.getName(0).toString();
                                String m = rel.getName(1).toString();
                                all.add(buildRecord(p, y, m));
                            }
                        });
                all.sort((a, b) -> ((String) b.get("time")).compareTo((String) a.get("time")));
                int lim = Math.min(limit, all.size());
                result = all.subList(0, lim);
            } catch (IOException e) {
                // 忽略错误
                logger.warn("遍历目录时出错: {}", e.getMessage());
            }
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("files", result);
        return ResponseEntity.ok(resp);
    }

    // 提供已上传的文件下载
    @GetMapping("/uploads/{year}/{month}/{filename:.+}")
    public ResponseEntity<?> serveFile(HttpSession session,
                                       @PathVariable String year,
                                       @PathVariable String month,
                                       @PathVariable String filename) throws IOException {

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("存储目录未找到，无法提供文件");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("存储目录未找到");
        }

        Path basePath = Paths.get(storageDir).toAbsolutePath().normalize();
        Path target;
        try {
            // 先构造相对路径，再从 basePath 解析，最后归一化
            Path rel = Paths.get(year, month, filename);
            target = basePath.resolve(rel).toAbsolutePath().normalize();
        } catch (InvalidPathException | NullPointerException e) {
            logger.warn("非法的文件路径请求: {}/{}/{} - {}", year, month, filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("无效的文件路径");
        }

        // 确保目标路径在存储目录内，防止路径遍历
        if (!target.startsWith(basePath)) {
            logger.warn("路径遍历尝试: {}", target);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("无效的文件路径");
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在");
        }
        String contentType = FileUtils.getContentTypeByExtension(filename);
        PathResource resource = new PathResource(target.toAbsolutePath().toString());
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mt = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
        }
        return ResponseEntity.ok()
                .contentType(mt)
                .header("Content-Disposition", "attachment; filename=" + FileUtils.encodeFilenameForHttpHeader(filename))
                .body(resource);
       
    }

    // 辅助方法：构建文件记录映射
    private Map<String, Object> buildRecord(Path fullPath, String y, String m) {
        Map<String, Object> map = new HashMap<>();
        try {
            File f = fullPath.toFile();
            long mtime = f.lastModified();
            Date d = new Date(mtime);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String mtimeS = fmt.format(d);
            String filename = fullPath.getFileName().toString();
            String ftype = FileUtils.getFileTypeCategory(filename);
            String content = null;
            if ("text".equals(ftype)) {
                try {
                    byte[] bytes = Files.readAllBytes(fullPath);
                    String txt = new String(bytes, StandardCharsets.UTF_8);
                    content = txt.length() > 500 ? txt.substring(0, 500) : txt;
                } catch (IOException e) {
                    content = null;
                }
            }
            map.put("year", y);
            map.put("month", m);
            map.put("filename", filename);
            map.put("url", "/uploads/" + y + "/" + m + "/" + filename);
            map.put("time", mtimeS);
            map.put("type", ftype);
            map.put("content", content);
        } catch (Exception e) {
            logger.warn("构建文件记录时出错: {}", e.getMessage(), e);
        }
        return map;
    }

}
