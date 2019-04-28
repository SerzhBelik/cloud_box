package ru.geekbrains.belikov.cloud.common;

import java.util.List;

public class FileList extends CommandMessage{
    private List<String> fileList;

    public FileList(List<String> fileList){
        this.fileList = fileList;
    }
}
