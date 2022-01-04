package ru.mralexeimk.objector.controllers;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.mralexeimk.objector.models.NeuralNetwork;
import ru.mralexeimk.objector.other.NewPageState;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;
import ru.mralexeimk.objector.singletons.Objects;
import ru.mralexeimk.objector.other.TableObject;
import ru.mralexeimk.objector.other.WebCamState;
import ru.mralexeimk.objector.singletons.SettingsListener;

import java.io.File;
import java.io.IOException;
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
    private Button delete, add, addCategory;
    @FXML
    private Label error;

    public void tableInit() {
        list.getItems().clear();
        NeuralNetwork nn = NeuralNetworkListener.get();
        for(String obj : nn.getObjects()) {
            list.getItems().add(new TableObject(obj, nn.getConfiguration(), nn.getLearningRate()));
        }
    }

    public void tableUpdate() {
        try {
            updateCategories();
            NeuralNetwork nn = NeuralNetworkListener.get();
            Set<String> list_objects = new HashSet<>();
            for(TableObject to : list.getItems()) {
                list_objects.add(to.getId());
            }
            if (!nn.getObjects().containsAll(list_objects) || !list_objects.containsAll(nn.getObjects())) {
                error.setText("");
                delete.setDisable(false);
                add.setDisable(false);

                tableInit();
            }
        } catch (Exception e) {}
    }

    @FXML
    public void initialize() {
        SettingsListener.init();
        File file = new File("weights/default.w");
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException ignored) {}

        file = new File("settings.objector");
        try {
            file.createNewFile();
        } catch (IOException ignored) {}

        listId.setCellValueFactory(new PropertyValueFactory<>("id"));
        listConfiguration.setCellValueFactory(new PropertyValueFactory<>("configuration"));
        listLr.setCellValueFactory(new PropertyValueFactory<>("lr"));

        updateCategories();
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
        String key = category.getValue().toString();
        NeuralNetworkListener.init(key);
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
    protected void onClickAddCategory() {
        openNewPage(NewPageState.ADD_CATEGORY);
    }
    @FXML
    protected void onClickOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("weights/"));
        fileChooser.showOpenDialog(list.getScene().getWindow());
    }
    @FXML
    protected void onClickAdd() {
        openNewPage(NewPageState.ADD_OBJECT);
    }
    @FXML
    protected void onClickDelete() {
        if(list.getSelectionModel() != null) {
            TableObject to = list.getSelectionModel().getSelectedItem();
            if (to != null) {
                NeuralNetworkListener.get().removeObject(to.getId());
                delete.setDisable(true);
            }
        }
    }
    @FXML
    protected void onClickSettings() {
        openSettings();
    }

    public void updateCategories() {
        File file = new File("weights/");
        for(File fe : file.listFiles()) {
            if(!fe.isDirectory()) {
                String name = fe.getName().split("\\.")[0];
                if(!category.getItems().contains(name)) {
                    category.getItems().add(name);
                    category.setValue(name);
                }
            }
        }
    }

    public void openNewPage(NewPageState state) {
        if(!NewPageController.isOpen) {
            try {
                FXMLLoader root = new FXMLLoader(getClass().getResource("/fxml/newPage.fxml"));
                Stage stage = new Stage();
                stage.setTitle("Добавить новый объект");
                stage.setScene(new Scene(root.load()));
                NewPageController controller = root.getController();
                controller.setState(state);
                stage.setOnHidden(e -> controller.close());
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void openSettings() {
        if(!SettingsController.isOpen) {
            try {
                FXMLLoader root = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(root.load()));
                stage.setTitle("Настройки");
                SettingsController controller = root.getController();
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