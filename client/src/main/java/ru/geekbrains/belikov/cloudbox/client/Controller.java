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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Controller implements Initializable {
    private String currentItemSelected;
    private static String CURRENT_DIRECTORY = "client_storage/";
    private static String CURRENT_SERVER_DIRECTORY = "";
    private Stack<String> localPathStack = new Stack<>();
    private Stack<String> serverPathStack = new Stack<>();
    private Refresh refresh;
    private List<String> fileList;
    private Map<String, Boolean> serverFilesMap;

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
        serverPathStack.push("");

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

    private void setDoubleClick(ListView<String> fileViewList) {
        fileViewList.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent click) {

                if (click.getClickCount() == 2) {
                    currentItemSelected = fileViewList.getSelectionModel()
                            .getSelectedItem();
                    openAndShow(fileViewList, currentItemSelected);
                }
            }
        });
    }

    private void openAndShow(ListView<String> fileViewList, String s) {

        if (Files.isDirectory(Paths.get(CURRENT_DIRECTORY + s))
                && fileViewList == localFileList){
                localPathStack.push(CURRENT_DIRECTORY);
                CURRENT_DIRECTORY = CURRENT_DIRECTORY + s + "/";
                System.out.println("ROOT = " + CURRENT_DIRECTORY);
                refreshLocalFilesList();
                return;
            }

            if (fileViewList == serverFileList && serverFilesMap.get(s)){
                CURRENT_SERVER_DIRECTORY = CURRENT_SERVER_DIRECTORY + s + "/";
                Network.sendMsg(new VistCommand(CURRENT_SERVER_DIRECTORY));
            }
        }


    private void selectMessage(AbstractMessage am) throws IOException{
        if (am instanceof FileMessage) {
            saveMessage(am);
        }

        if (am instanceof FileMap){
            FileMap fileMap = (FileMap) am;
            refreshServerFilesList(fileMap);
        }
    }

    private void refreshServerFilesList(FileMap fileMap) {
        serverFilesMap = fileMap.getFileMap();
        fileList = new ArrayList<String>(fileMap.getFileMap().keySet());
        if (Platform.isFxApplicationThread()) {
                serverFileList.getItems().clear();
                fileList.forEach(o -> serverFileList.getItems().add(o));
        } else {
            Platform.runLater(() -> {
                    serverFileList.getItems().clear();
                    fileList.forEach(o -> serverFileList.getItems().add(o));
            });
        }
    }

    private void executeCommand(CommandMessage cm) {
        //FIXME
    }

    private void saveMessage(AbstractMessage am) throws IOException{
        FileMessage fm = (FileMessage) am;
        Files.write(Paths.get(CURRENT_DIRECTORY + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
        refreshLocalFilesList();
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                localFileList.getItems().clear();
                Files.list(Paths.get(CURRENT_DIRECTORY)).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    localFileList.getItems().clear();
                    Files.list(Paths.get(CURRENT_DIRECTORY)).map(p -> p.getFileName().toString()).forEach(o -> localFileList.getItems().add(o));
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
            FileController.delete(CURRENT_DIRECTORY + item + "/");
//            delete(CURRENT_DIRECTORY + item + "/");
        }
        refreshLocalFilesList();
    }



    public void btnServDelete(ActionEvent actionEvent) {
        MultipleSelectionModel<String> msm= serverFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
            Network.sendMsg(new Delete(Paths.get(item)));
        }
        Network.sendMsg(new Refresh());
    }


    public void btnSend(ActionEvent actionEvent) throws IOException{

        MultipleSelectionModel<String> msm= localFileList.getSelectionModel();
        ObservableList<String> selected = msm.getSelectedItems();
        for (String item : selected) {
//            FileController.send(CURRENT_DIRECTORY + item + "/");
            send(item + "/");
//            Network.sendMsg(new FileMessage(Paths.get(CURRENT_DIRECTORY + item)));
        }
        Network.sendMsg(new Refresh());
    }

    private void send(String item) {
        System.out.println(item);

        if (Files.isDirectory(Paths.get(CURRENT_DIRECTORY + item))){
            System.out.println("to send " + item );
            Network.sendMsg(new FileMessage(item, CURRENT_DIRECTORY, true));
            File dir = new File(CURRENT_DIRECTORY + item);
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return;
            }

            for (File f: files
                 ) {
//                System.out.println(item + f.getName());
                System.out.println("to send " + item + f.getName() + "/");
                send(item + f.getName() + "/");
            }
//            Network.sendMsg(new Up());
        } else {
            System.out.println("item = " + item);
            Network.sendMsg(new FileMessage(item, CURRENT_DIRECTORY, false));
            return;
        }
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
        CURRENT_DIRECTORY = localPathStack.pop();
        refreshLocalFilesList();
    }

    public void upServ(){
//        refresh = new Refresh();
//        refresh.setUp(true);
        Network.sendMsg(new Up());
        Network.sendMsg(new Refresh());
    }
}
