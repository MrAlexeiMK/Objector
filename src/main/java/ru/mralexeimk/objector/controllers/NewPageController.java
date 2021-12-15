package ru.mralexeimk.objector.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NewPageController {
    private MainController mainController;
    public static boolean isOpen = false;
    @FXML
    private Label label;
    @FXML
    private Button back, ok;
    @FXML
    private TextField field;

    @FXML
    public void initialize() {
        isOpen = true;
    }

    @FXML
    public void onClickBack() {
        close();
    }

    @FXML
    public void onClickOK() {
        if(!field.getText().isEmpty()) {
            MainController.objects.addObject(field.getText());
            close();
        }
    }

    public void close() {
        Stage stage = (Stage) label.getScene().getWindow();
        stage.close();
        isOpen = false;
    }
}
