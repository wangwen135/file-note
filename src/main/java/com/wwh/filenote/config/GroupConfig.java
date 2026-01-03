package com.wwh.filenote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "file-note")
public class GroupConfig {
    private Anonymous anonymous;
    private List<Group> groups;

    public Anonymous getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Anonymous anonymous) {
        this.anonymous = anonymous;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public static class Anonymous {
        private boolean enabled;
        private String name;
        private String directory;
        private String role;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class Group {
        private String name;
        private String directory;
        private List<User> users;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
        public List<User> getUsers() { return users; }
        public void setUsers(List<User> users) { this.users = users; }
    }

    public static class User {
        private String username;
        private String password;
        private String role; // admin or user

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}