package ru.geekbrains.belikov.cloudbox.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.MouseEvent;
import ru.geekbrains.belikov.cloud.common.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;
import java.util.Stack;

public class Controller implements Initializable {
    private String currentItemSelected;
    private static String ROOT = "client_storage/";
    private Stack<String> localPathStack = new Stack<>();
    private Stack<String> serverPathStack = new Stack<>();

    @FXML
    ListView<String> localFileList;

    @FXML
    ListView<String> serverFileList;

//    @FXML
//    Button close_btn;
//
//    @FXML
//    Button del_serv_btn;
//
//    @FXML
//    Button update_serv_btn;
//
//    @FXML
//    Button update_clnt_btn;

    @FXML
    Button upLocal;

    @FXML
    Button upServ;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Network.start();
        Network.sendMsg(new Refresh());
        setDoubleClick(localFileList);
        setDoubleClick(serverFileList);

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

    private void setDoubleClick(ListView<String> FileList) {
        FileList.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent click) {

                if (click.getClickCount() == 2) {
                    //Use ListView's getSelected Item
                    String currentItemSelected = FileList.getSelectionModel()
                            .getSelectedItem();
                    //use this to do whatever you want to. Open Link etc.
                    System.out.println(currentItemSelected);
                    openAndShow(ROOT + currentItemSelected);
                }
            }
        });
    }

    private void openAndShow(String s) {
        if (Files.isDirectory(Paths.get(s))){
            localPathStack.push(ROOT);
            ROOT = s + "/";
            System.out.println("ROOT = " + ROOT);
            refreshLocalFilesList();
        }
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
        //FIXME
    }

    private void saveMessage(AbstractMessage am) throws IOException{
        FileMessage fm = (FileMessage) am;
        Files.write(Paths.get(ROOT + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
        refreshLocalFilesList();
//        System.out.println(ROOT);
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                localFileList.getItems().clear();
                Files.list(Paths.get(ROOT)).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));

//                for (String item: localFileList.getItems()
//                     ) {
//                    System.out.println(item);
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    localFileList.getItems().clear();
                    Files.list(Paths.get(ROOT)).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void btnClntDelete(ActionEvent actionEvent) throws IOException{
        MultipleSelectionModel<String> msm= localFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
            Files.delete(Paths.get(ROOT + item));
        }
        refreshLocalFilesList();
    }


    public void btnServDelete(ActionEvent actionEvent) {
        MultipleSelectionModel<String> msm= serverFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
            System.out.println(item);
            Network.sendMsg(new Delete(Paths.get(item)));
        }
        Network.sendMsg(new Refresh());
    }


    public void btnSend(ActionEvent actionEvent) throws IOException{

        MultipleSelectionModel<String> msm= localFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
            System.out.println(item);
            Network.sendMsg(new FileMessage(Paths.get(ROOT + item)));
        }
        Network.sendMsg(new Refresh());
    }


    public void btnExit(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void btnDownload(ActionEvent actionEvent) {
        MultipleSelectionModel<String> msm= serverFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
            Network.sendMsg(new FileRequest(item));
        }

    }

    public void btnUpdate(ActionEvent actionEvent) {

        refreshLocalFilesList();
        Network.sendMsg(new Refresh());

    }

    public void upLocal(){
        if (localPathStack.empty()) return;
        ROOT = localPathStack.pop();
        refreshLocalFilesList();
    }

    public void upServ(){

    }
}
