package ru.geekbrains.belikov.cloud.common;

import java.nio.file.Path;


public class Delete extends CommandMessage {
    private final String fileName;
    public Delete(Path path){
        this.fileName = path.getFileName().toString();
    }

    public String getFileName() {
        return fileName;
    }
}
