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
import ru.mralexeimk.objector.singletons.Objects;
import ru.mralexeimk.objector.other.TableObject;
import ru.mralexeimk.objector.other.WebCamState;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainController {
    @FXML
    private TableView<TableObject> list;
    @FXML
    private TableColumn<TableObject, String> listId;
    @FXML
    private TableColumn<TableObject, String> listConfiguration;
    @FXML
    private TableColumn<TableObject, Double> listLr;
    @FXML
    private ChoiceBox category;
    @FXML
    private Button delete, add;
    @FXML
    private Label error;

    public void tableInit() {
        list.getItems().clear();
        String key = category.getValue().toString();
        Objects.init(key);
        for(String obj : Objects.getObjects()) {
            NeuralNetwork nn = Objects.getNeuralNetwork(obj);
            list.getItems().add(new TableObject(obj, nn.getConfiguration(), nn.getLearningRate()));
        }
    }

    public void tableUpdate() {
        try {
            Set<String> list_objects = new HashSet<>();
            for(TableObject to : list.getItems()) {
                list_objects.add(to.getId());
            }
            if (!Objects.getObjects().containsAll(list_objects) || !list_objects.containsAll(Objects.getObjects())) {
                error.setText("");
                delete.setDisable(false);
                add.setDisable(false);

                for(String obj : Objects.getObjects()) {
                    if(!list_objects.contains(obj)) {
                        NeuralNetwork nn = Objects.getNeuralNetwork(obj);
                        list.getItems().add(new TableObject(obj, nn.getConfiguration(), nn.getLearningRate()));
                    }
                }
                list.getItems().removeIf(to -> !Objects.getObjects().contains(to.getId()));
            }
        } catch (Exception e) {}
    }

    @FXML
    public void initialize() {
        listId.setCellValueFactory(new PropertyValueFactory<>("id"));
        listConfiguration.setCellValueFactory(new PropertyValueFactory<>("configuration"));
        listLr.setCellValueFactory(new PropertyValueFactory<>("lr"));
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
        else error.setText("Нет объектов");
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
                Objects.deleteObject(to.getId());
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