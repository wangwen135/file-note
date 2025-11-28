package com.example.pasteboard.controller;

import com.example.pasteboard.config.GroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;
import java.net.URI;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private GroupConfig groupConfig;

    @PostMapping("/login")
    public ResponseEntity<?> loginPost(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session) {

        logger.info("Login attempt for user: {}", username);
        // 查找用户所属的组和角色
        GroupConfig.Group userGroup = null;
        GroupConfig.User currentUser = null;
        for (GroupConfig.Group group : groupConfig.getGroups()) {
            for (GroupConfig.User user : group.getUsers()) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    userGroup = group;
                    currentUser = user;
                    break;
                }
            }
            if (currentUser != null) break;
        }

        if (currentUser == null) {
            logger.warn("Login failed for user: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }

        // 将用户信息存储到session
        session.setAttribute("username", currentUser.getUsername());
        session.setAttribute("groupName", userGroup.getName());
        session.setAttribute("storageDir", userGroup.getDirectory());
        session.setAttribute("role", currentUser.getRole());
        session.setAttribute("logged_in", true);

        logger.info("User {} logged in successfully, group: {}", username, userGroup.getName());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            logger.info("User {} logged out", username);
        }
        session.invalidate();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login")).build();
    }

    @GetMapping("/api/user")
    @ResponseBody
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("groupName", (String) session.getAttribute("groupName"));
        userInfo.put("role", (String) session.getAttribute("role"));
        return ResponseEntity.ok(userInfo);
    }
}