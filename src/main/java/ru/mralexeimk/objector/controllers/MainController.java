package ru.mralexeimk.objector.controllers;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.mralexeimk.objector.models.NeuralNetwork;
import ru.mralexeimk.objector.models.Objects;
import ru.mralexeimk.objector.other.TableObject;
import ru.mralexeimk.objector.other.WebCamState;

import java.io.File;
import java.util.List;

public class MainController {
    @FXML
    private TableView<TableObject> list;
    @FXML
    private TableColumn<TableObject, String> listId;
    @FXML
    private TableColumn<TableObject, List<Integer>> listLayers;
    @FXML
    private TableColumn<TableObject, Double> listLr;
    @FXML
    private ChoiceBox category;
    @FXML
    private Button delete;
    @FXML
    private Label error;

    public static Objects objects;

    public void tableInit() {
        list.getItems().clear();
        String key = category.getValue().toString();
        objects = new Objects(key);
        for(String obj : objects.getObjects()) {
            NeuralNetwork nn = objects.getNeuralNetwork(obj);
            list.getItems().add(new TableObject(obj, nn.getLayers(), nn.getLearningRate()));
        }
    }

    public void tableUpdate() {
        try {
            if (objects.getObjects().size() != list.getItems().size()) {
                error.setText("");
                tableInit();
                delete.setDisable(false);
            }
        } catch (Exception e) {}
    }

    @FXML
    public void initialize() {
        listId.setCellValueFactory(new PropertyValueFactory<TableObject, String>("id"));
        listLayers.setCellValueFactory(new PropertyValueFactory<TableObject, List<Integer>>("layers"));
        listLr.setCellValueFactory(new PropertyValueFactory<TableObject, Double>("lr"));
        File file = new File("weights/");
        file.mkdirs();
        File file2 = new File("weights/default/");
        file2.mkdirs();
        for(File fe : file.listFiles()) {
            if(fe.isDirectory()) {
                category.getItems().add(fe.getName());
            }
        }
        if(!category.getItems().isEmpty()) {
            category.setValue(category.getItems().get(0));
        }

        TimerService service = new TimerService();
        service.setPeriod(Duration.millis(100));
        service.setOnSucceeded(t -> {
            tableUpdate();
        });
        service.start();
    }
    @FXML
    public void changeCategory() {
        tableInit();
    }
    @FXML
    protected void onClickTrain() {
        if(!list.getItems().isEmpty()) {
            openWebCam(WebCamState.TRAIN);
        }
        else error.setText("Отсутствуют объекты");
    }
    @FXML
    protected void onClickQuery() {
        if(!list.getItems().isEmpty()) {
            openWebCam(WebCamState.QUERY);
        }
        else error.setText("Отсутствуют объекты");
    }
    @FXML
    protected void onClickOpen() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("weights/"));
        directoryChooser.showDialog(list.getScene().getWindow());
    }
    @FXML
    protected void onClickAdd() {
        openNewPage();
    }
    @FXML
    protected void onClickDelete() {
        if(list.getSelectionModel() != null) {
            TableObject to = list.getSelectionModel().getSelectedItem();
            if (to != null) {
                objects.deleteObject(to.getId());
                delete.setDisable(true);
            }
        }
    }
    @FXML
    protected void onClickSettings() {

    }

    public void openNewPage() {
        if(!NewPageController.isOpen) {
            try {
                FXMLLoader root = new FXMLLoader(getClass().getResource("/fxml/newPage.fxml"));
                Stage stage = new Stage();
                stage.setTitle("Добавить новый объект");
                stage.setScene(new Scene(root.load()));
                NewPageController controller = root.getController();
                stage.setOnHidden(e -> controller.close());
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void openWebCam(WebCamState state) {
        if(!WebCamController.isOpen) {
            try {
                FXMLLoader root = new FXMLLoader(getClass().getResource("/fxml/webCam.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(root.load()));
                stage.setTitle("Обучение объекту");
                if(state == WebCamState.QUERY) stage.setTitle("Распознавание объекта");
                WebCamController controller = root.getController();
                controller.setState(state);
                stage.setOnHidden(e -> controller.close());
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class TimerService extends ScheduledService<Boolean> {
        protected Task<Boolean> createTask() {
            return new Task<>() {
                protected Boolean call() {
                    return null;
                }
            };
        }
    }
}