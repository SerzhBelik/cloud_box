package ru.geekbrains.belikov.cloudbox.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.geekbrains.belikov.cloud.common.Auth;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginController{
    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    VBox globParent;

    public int id;

//    public Controller backController;

    public void auth(ActionEvent actionEvent) {

        if (isAuth()) {
            System.out.println(login.getText() + " " + password.getText());
            System.out.println("id = " + id);
            globParent.getScene().getWindow().hide();


            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Parent root = null;
            try {
                root = fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Stage primaryStage = new Stage();
            primaryStage.setTitle("Box Client");
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);

            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText("Не верный логин или пароль!");

            alert.showAndWait();
        }


    }



    private boolean isAuth() {
        Network.start();
        Network.sendMsg(new Auth(login.getText(), password.getText()));
        Object om;
            try {
                do  {
                    om = Network.readObject();
                    if (om instanceof Auth) {
                        System.out.println(((Auth) om).isAuth());
                        return ((Auth) om).isAuth();
                    }

                    } while (om instanceof Auth);

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }


        return false; // FIXME
    }

}
