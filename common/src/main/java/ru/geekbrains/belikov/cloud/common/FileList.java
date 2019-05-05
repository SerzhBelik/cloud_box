package ru.geekbrains.belikov.cloud.common;

import java.util.List;

public class FileList extends AbstractMessage{
    private List<String> fileList;

    public FileList(List<String> fileList){
        this.fileList = fileList;
    }

    public List<String> getFileList() {
        return fileList;
    }
}
