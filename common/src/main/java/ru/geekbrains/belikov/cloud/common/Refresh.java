package ru.geekbrains.belikov.cloud.common;

public class Refresh extends CommandMessage {
    private Boolean isUp = false;

    public Boolean getUp() {
        return isUp;
    }

    public void setUp(Boolean up) {
        isUp = up;
    }
}
