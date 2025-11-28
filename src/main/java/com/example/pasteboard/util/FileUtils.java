package com.example.pasteboard.util;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class FileUtils {

    // Sanitize filename: remove path components, nulls, control chars, replace slashes, collapse whitespace, trim dots, limit length.
    public static String safeUnicodeFilename(String name) {
        if (name == null) name = "file";
        // Basename
        name = Paths.get(name).getFileName().toString();
        // Remove nulls
        name = name.replace("\0", "");
        // Replace separators
        name = name.replace("/", "_").replace("\\", "_");
        // Collapse whitespace
        name = name.trim().replaceAll("\\s+", " ");
        // Trim surrounding dots/spaces
        name = name.replaceAll("^[\\.\\s]+", "").replaceAll("[\\.\\s]+$", "");
        if (name.isEmpty()) name = "file";
        // Normalize to NFC to keep Unicode sensible
        name = Normalizer.normalize(name, Normalizer.Form.NFC);
        // Limit length while preserving extension
        int maxTotal = 200;
        if (name.length() > maxTotal) {
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                String ext = name.substring(dot);
                int maxBase = maxTotal - ext.length();
                name = name.substring(0, Math.min(name.length(), maxBase)) + ext;
            } else {
                name = name.substring(0, maxTotal);
            }
        }
        // Remove control characters
        name = name.replaceAll("[\\p{Cntrl}]", "");
        // Final fallback
        if (name.isEmpty()) name = "file";
        return name;
    }
}
