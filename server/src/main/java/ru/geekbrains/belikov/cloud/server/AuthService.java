package ru.geekbrains.belikov.cloud.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthService {

    public static boolean checkUser(String login, String password) {
        return login.equals("login") && password.equals("pass"); // FIXME

    }
}
