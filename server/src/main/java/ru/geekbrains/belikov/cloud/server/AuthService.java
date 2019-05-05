package ru.geekbrains.belikov.cloud.server;


public class AuthService {

    public static boolean checkUser(String login, String password) {
        return (login.equals("user1") && password.equals("pass1"))||
                (login.equals("user2") && password.equals("pass2")); // FIXME

    }
}
