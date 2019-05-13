package ru.geekbrains.belikov.cloud.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;
    private boolean isDirectory = false;

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public FileMessage(Path path) throws IOException {
        filename = path.getFileName().toString();
        data = Files.readAllBytes(path);
    }

    public FileMessage(String path, String prefix, boolean isDirectory){
        this.isDirectory = isDirectory;
        filename = path;

        if (!isDirectory) {
            try {
                data = Files.readAllBytes(Paths.get(prefix + path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
