package ru.geekbrains.belikov.cloudbox.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import ru.geekbrains.belikov.cloud.common.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> localFileList;

    @FXML
    ListView<String> serverFileList;

    @FXML
    Button close_btn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Network.sendMsg(new Refresh());
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Object om = Network.readObject();
                    if (om instanceof AbstractMessage){
                        AbstractMessage am = (AbstractMessage) om;
                        selectMessage(am);
                    }


                    if (om instanceof CommandMessage){
                        CommandMessage cm = (CommandMessage) om;
                        executeCommand(cm);
                    }

                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        refreshLocalFilesList();
    }

    private void selectMessage(AbstractMessage am) throws IOException{
        if (am instanceof FileMessage) {
            saveMessage(am);
        }

        if (am instanceof FileList){
            FileList fileList = (FileList) am;
            refreshServerFilesList(fileList);
        }
    }

    private void refreshServerFilesList(FileList fileList) {
        System.out.println(fileList.getFileList());
        if (Platform.isFxApplicationThread()) {
                serverFileList.getItems().clear();
                fileList.getFileList().forEach(o -> serverFileList.getItems().add(o));
        } else {
            Platform.runLater(() -> {
                    serverFileList.getItems().clear();
                    fileList.getFileList().forEach(o -> serverFileList.getItems().add(o));
            });
        }
    }

    private void executeCommand(CommandMessage cm) {

    }

    private void saveMessage(AbstractMessage am) throws IOException{
        FileMessage fm = (FileMessage) am;
        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
        refreshLocalFilesList();
//                        обратная передача файлов
        System.out.println("отправка на сервер");
        Network.sendMsg(new FileMessage(Paths.get("client_storage/2.txt")));
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfFileName.getText()));
            tfFileName.clear();
        }
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                localFileList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    localFileList.getItems().clear();
                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void btnShowAlert(ActionEvent actionEvent) {

    }

    public void btnSend(ActionEvent actionEvent) throws IOException{

        MultipleSelectionModel<String> msm= localFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
            Network.sendMsg(new FileMessage(Paths.get("client_storage/" + item)));
        }
        Network.sendMsg(new Refresh());
    }


    public void btnExit(ActionEvent actionEvent) {
        System.exit(0);
    }
}
