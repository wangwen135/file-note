package com.example.pasteboard.controller;

import com.example.pasteboard.util.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Controller
public class PasteboardController {

    private static final Logger logger = LoggerFactory.getLogger(PasteboardController.class);
    private Tika tika = new Tika();

    @PostMapping("/upload_file")
    @ResponseBody
    public ResponseEntity<String> uploadFile(HttpSession session, @RequestParam("file") MultipartFile[] files) throws IOException {
        String username = (String) session.getAttribute("username");
        if (username == null) username = "unknown";
        logger.info("User {} attempting to upload {} files", username, files.length);

        if (session.getAttribute("logged_in") == null) {
            logger.warn("Unauthorized file upload attempt by {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未授权访问");
        }

        if (files == null || files.length == 0) {
            logger.warn("User {} upload failed: no files selected", username);
            return ResponseEntity.badRequest().body("未选择文件");
        }

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("User {} upload failed: storage directory not found", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未授权访问");
        }

        Path uploadDir = Paths.get(storageDir, new SimpleDateFormat("yyyy").format(new Date()), new SimpleDateFormat("MM").format(new Date()));
        Files.createDirectories(uploadDir);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());

        for (MultipartFile f : files) {
            String filename = FileUtils.safeUnicodeFilename(f.getOriginalFilename() != null ? f.getOriginalFilename() : "pasted_file");
            String savedFilename = filename.substring(0, filename.lastIndexOf('.')) + "_" + timestamp + filename.substring(filename.lastIndexOf('.'));
            Path savePath = uploadDir.resolve(savedFilename);
            Files.copy(f.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("User {} uploaded file: {}", username, savedFilename);
        }

        return ResponseEntity.ok("OK");
    }

    @PostMapping("/upload_text")
    @ResponseBody
    public ResponseEntity<String> uploadText(HttpSession session, @RequestBody Map<String, Object> body) throws IOException {
        String username = (String) session.getAttribute("username");
        if (username == null) username = "unknown";
        logger.info("User {} attempting to upload text", username);

        if (session.getAttribute("logged_in") == null) {
            logger.warn("Unauthorized text upload attempt by {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未授权访问");
        }

        if (body == null || !body.containsKey("text") || ((String) body.get("text")).trim().isEmpty()) {
            logger.warn("User {} text upload failed: empty content", username);
            return ResponseEntity.badRequest().body("无文本内容");
        }

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("User {} text upload failed: storage directory not found", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未授权访问");
        }

        Path uploadDir = Paths.get(storageDir, new SimpleDateFormat("yyyy").format(new Date()), new SimpleDateFormat("MM").format(new Date()));
        Files.createDirectories(uploadDir);
        String filename = "paste_" + new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date()) + ".txt";
        Path savePath = uploadDir.resolve(filename);
        Files.write(savePath, ((String) body.get("text")).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        logger.info("User {} uploaded text file: {}", username, filename);

        return ResponseEntity.ok("OK");
    }

    @DeleteMapping("/delete_file")
    @ResponseBody
    public ResponseEntity<String> deleteFile(@RequestParam("path") String filePath, HttpSession session) throws IOException {
        String username = (String) session.getAttribute("username");
        if (username == null) username = "unknown";
        logger.info("User {} attempting to delete file: {}", username, filePath);

        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role)) {
            logger.warn("User {} unauthorized deletion attempt: {}", username, filePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无删除权限");
        }

        String storageDir = (String) session.getAttribute("storageDir");
        if (storageDir == null) {
            logger.error("User {} deletion failed: storage directory not found", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未授权访问");
        }

        Path basePath = Paths.get(storageDir).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(filePath).normalize();

        if (!targetPath.startsWith(basePath)) {
            logger.warn("User {} attempted path traversal: {}", username, filePath);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("无效的文件路径");
        }

        if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
            Files.delete(targetPath);
            logger.info("User {} deleted file successfully: {}", username, filePath);
            return ResponseEntity.ok("文件删除成功");
        } else {
            logger.warn("User {} deletion failed: file not found - {}", username, filePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在");
        }
    }

    private String fileTypeFromExt(String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) ext = filename.substring(dot + 1).toLowerCase();
        if (Arrays.asList("png", "jpg", "jpeg", "gif", "webp").contains(ext)) return "image";
        if (Arrays.asList("mp4", "webm", "ogg").contains(ext)) return "video";
        if ("txt".equals(ext)) return "text";
        return "file";
    }


    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }
}
