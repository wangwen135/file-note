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

        if (groupConfig.getGroups() == null || groupConfig.getGroups().isEmpty()) {
            throw new IllegalArgumentException("application.yml中未配置任何组");
        }

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

        logger.info("配置验证成功完成");
    }
}