package ru.geekbrains.belikov.cloud.common;

public class VistCommand extends CommandMessage {
    private String DIRECTORY;

    public String getDirectory() {
        return DIRECTORY;
    }

    public VistCommand(String directory){
        DIRECTORY = directory;
    }
}
