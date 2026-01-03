package com.wwh.filenote.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ConfigValidationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ConfigValidationRunner.class);

    private final GroupConfig groupConfig;

    @Autowired
    public ConfigValidationRunner(GroupConfig groupConfig) {
        this.groupConfig = groupConfig;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始配置验证...");

        boolean hasValidGroups = false;

        // 验证普通用户组配置（如果有配置）
        if (groupConfig.getGroups() != null && !groupConfig.getGroups().isEmpty()) {
            hasValidGroups = true;
            for (GroupConfig.Group group : groupConfig.getGroups()) {
                // 输出组信息（不含密码）
                logger.info("正在验证组: {}", group.getName());
                logger.info("  目录: {}", group.getDirectory());
                logger.info("  用户数量: {}", group.getUsers() != null ? group.getUsers().size() : 0);
                if (group.getUsers() != null) {
                    group.getUsers().forEach(user -> logger.info("    用户: {}", user.getUsername()));
                }

                // 验证目录
                if (group.getDirectory() == null || group.getDirectory().trim().isEmpty()) {
                    throw new IllegalArgumentException("组未配置目录: " + group.getName());
                }

                Path directoryPath = Paths.get(group.getDirectory());
                if (!Files.exists(directoryPath)) {
                    logger.info("正在创建目录: {}", directoryPath);
                    Files.createDirectories(directoryPath);
                } else if (!Files.isDirectory(directoryPath)) {
                    throw new IllegalArgumentException("路径不是目录: " + directoryPath);
                }
            }
        }

        // 验证匿名用户组配置
        GroupConfig.Anonymous anonymous = groupConfig.getAnonymous();
        if (anonymous != null && anonymous.isEnabled()) {
            hasValidGroups = true;
            logger.info("正在验证匿名用户组配置");
            logger.info("  名称: {}", anonymous.getName());
            logger.info("  目录: {}", anonymous.getDirectory());
            logger.info("  角色: {}", anonymous.getRole());

            // 验证匿名用户组的必要配置
            if (anonymous.getName() == null || anonymous.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("匿名用户组未配置名称");
            }

            if (anonymous.getDirectory() == null || anonymous.getDirectory().trim().isEmpty()) {
                throw new IllegalArgumentException("匿名用户组未配置目录");
            }

            if (anonymous.getRole() == null || anonymous.getRole().trim().isEmpty()) {
                throw new IllegalArgumentException("匿名用户组未配置角色");
            }

            // 验证并创建匿名用户目录
            Path anonymousDirectoryPath = Paths.get(anonymous.getDirectory());
            if (!Files.exists(anonymousDirectoryPath)) {
                logger.info("正在创建匿名用户目录: {}", anonymousDirectoryPath);
                Files.createDirectories(anonymousDirectoryPath);
            } else if (!Files.isDirectory(anonymousDirectoryPath)) {
                throw new IllegalArgumentException("匿名用户组路径不是目录: " + anonymousDirectoryPath);
            }
        } else if (anonymous != null) {
            logger.info("匿名用户组已配置但未启用");
        } else {
            logger.info("未配置匿名用户组");
        }

        // 检查是否至少有一个有效配置
        if (!hasValidGroups) {
            throw new IllegalArgumentException("application.yml中未配置任何有效组（普通用户组或匿名用户组）");
        }

        logger.info("配置验证成功完成");
    }
}