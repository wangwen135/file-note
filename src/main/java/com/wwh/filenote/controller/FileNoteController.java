package com.wwh.filenote.controller;

import com.wwh.filenote.util.FileUtils;
import org.apache.tika.Tika;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class FileNoteController {

    private static final Logger logger = LoggerFactory.getLogger(FileNoteController.class);
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

    @GetMapping("/list_periods")
    @ResponseBody
    public ResponseEntity<?> listPeriods(HttpSession session) throws IOException {
        if (session.getAttribute("logged_in") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
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
                        // sort descending numeric
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
        if (session.getAttribute("logged_in") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
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
                // sort by time desc
                result.sort((a, b) -> ((String) b.get("time")).compareTo((String) a.get("time")));
            }
        } else {
            // walk and collect
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
                // ignore
                logger.warn("Error walking directory: {}", e.getMessage());
            }
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("files", result);
        return ResponseEntity.ok(resp);
    }

    // Serve uploaded files
    @GetMapping("/uploads/{year}/{month}/{filename:.+}")
    public ResponseEntity<?> serveUpload(HttpSession session,
                                         @PathVariable String year,
                                         @PathVariable String month,
                                         @PathVariable String filename) throws IOException {
        if (session.getAttribute("logged_in") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        String storageDir = (String) session.getAttribute("storageDir");

        Path file = Paths.get(storageDir, year, month, filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
        }
        String contentType = tika.detect(file.toFile());
        PathResource resource = new PathResource(file.toAbsolutePath().toString());
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mt = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
        }
        return ResponseEntity.ok()
                .contentType(mt)
                .body(resource);
    }

    // helper to build record map
    private Map<String, Object> buildRecord(Path fullPath, String y, String m) {
        Map<String, Object> map = new HashMap<>();
        try {
            File f = fullPath.toFile();
            long mtime = f.lastModified();
            Date d = new Date(mtime);
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String mtimeS = fmt.format(d);
            String filename = fullPath.getFileName().toString();
            String ftype = fileTypeFromExt(filename);
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
            // ignore
        }
        return map;
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
