package com.wwh.filenote.controller;

import com.wwh.filenote.config.GroupConfig;
import com.wwh.filenote.security.LoginAttemptManager;
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

/**
 * 认证控制器，处理用户登录、登出和用户信息获取等功能
 */
@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private GroupConfig groupConfig;

    @Autowired
    private LoginAttemptManager loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<?> loginPost(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session) {

        logger.info("用户登录尝试: {}", username);

        // 检查用户是否被锁定
        if (loginAttemptService.isLocked(username)) {
            long remainingLockTime = loginAttemptService.getRemainingLockTime(username);
            logger.warn("用户 {} 已被锁定，剩余锁定时间: {} 秒", username, remainingLockTime);
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body("登录失败次数过多，请" + remainingLockTime + "秒后重试");
        }

        // 查找用户所属的组和角色
        GroupConfig.Group userGroup = null;
        GroupConfig.User currentUser = null;
        if (groupConfig.getGroups() != null) {
            // 遍历所有组和用户
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
        }

        if (currentUser == null) {
            // 登录失败，记录失败次数
            if (loginAttemptService.loginFailed(username)) {
                long remainingLockTime = loginAttemptService.getRemainingLockTime(username);
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body("登录失败次数过多，请" + remainingLockTime + "秒后重试");
            }

            logger.warn("用户登录失败: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }

        // 登录成功，重置失败次数
        loginAttemptService.loginSucceeded(username);

        // 将用户信息存储到session
        session.setAttribute("username", currentUser.getUsername());
        session.setAttribute("groupName", userGroup.getName());
        session.setAttribute("storageDir", userGroup.getDirectory());
        session.setAttribute("role", currentUser.getRole());
        session.setAttribute("logged_in", true);
        session.setAttribute("is_anonymous", false); // 明确设置为非匿名用户

        logger.info("用户 {} 登录成功, 所属组: {}", username, userGroup.getName());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/index.html")).build();
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            logger.info("用户 {} 已登出", username);
        }
        session.invalidate();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login.html")).build();
    }

    /**
     * 获取当前登录用户的信息
     *
     * @param session 当前HTTP会话
     * @return 包含用户信息的Map，包括组名、用户名、角色和是否为匿名用户
     */
    @GetMapping("/api/user")
    @ResponseBody
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("groupName", (String) session.getAttribute("groupName"));
        userInfo.put("username", (String) session.getAttribute("username"));
        userInfo.put("role", (String) session.getAttribute("role"));
        userInfo.put("isAnonymous", session.getAttribute("is_anonymous") != null && (Boolean) session.getAttribute("is_anonymous"));
        return ResponseEntity.ok(userInfo);
    }

    /**
     * 获取匿名访问配置信息
     *
     * @return 包含匿名访问配置的Map，包括是否启用
     */
    @GetMapping("/api/anonymous-config")
    @ResponseBody
    public ResponseEntity<?> getAnonymousConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", groupConfig.getAnonymous() != null && groupConfig.getAnonymous().isEnabled());
        return ResponseEntity.ok(config);
    }
}