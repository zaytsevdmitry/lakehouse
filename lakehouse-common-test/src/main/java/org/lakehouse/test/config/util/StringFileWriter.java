package org.lakehouse.test.config.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StringFileWriter {

    /**
     * Writes a string to a file. Overwrites the file if it exists.
     * Automatically creates any missing parent directories.
     *
     * @param fileName The name or path of the file
     * @param content  The string content to write
     * @throws IOException If an I/O error occurs
     */
    public static void writeString(String fileName, String content) throws IOException {
        if (fileName == null || content == null) {
            throw new IllegalArgumentException("File name and content cannot be null");
        }

        Path path = Paths.get(fileName);
        createParentDirectories(path);

        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /**
     * Appends a string to the end of the file.
     * Automatically creates any missing parent directories.
     */
    public static void appendString(String fileName, String content) throws IOException {
        if (fileName == null || content == null) {
            throw new IllegalArgumentException("File name and content cannot be null");
        }

        Path path = Paths.get(fileName);
        createParentDirectories(path);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8, true))) {
            writer.write(content);
        }
    }

    /**
     * Helper method to safely create the parent directory structure if it does not exist.
     */
    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
