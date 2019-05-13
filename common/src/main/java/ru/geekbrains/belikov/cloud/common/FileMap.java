package ru.geekbrains.belikov.cloud.common;

import java.util.List;
import java.util.Map;

public class FileMap extends AbstractMessage{
    private Map<String, Boolean> fileMap;

    public FileMap(Map<String, Boolean> fileList){
        this.fileMap = fileList;
    }

    public Map<String, Boolean> getFileMap() {
        return fileMap;
    }
}
