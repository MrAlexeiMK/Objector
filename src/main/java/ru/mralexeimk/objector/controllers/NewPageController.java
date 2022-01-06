package ru.mralexeimk.objector.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mralexeimk.objector.other.NewPageState;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;

import java.io.File;
import java.io.IOException;

public class NewPageController {
    public static boolean isOpen = false;
    private NewPageState state = null;
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
        if(state != null) {
            if (!field.getText().isEmpty()) {
                String text = field.getText();
                if(state == NewPageState.ADD_OBJECT) {
                    NeuralNetworkListener.get().addObject(text);
                }
                else if(state == NewPageState.ADD_CATEGORY) {
                    File file = new File("weights/"+text+".w");
                    file.getParentFile().mkdirs();
                    try {
                        file.createNewFile();
                    } catch (IOException ignored) {}
                }
                close();
            }
        }
    }

    public void setState(NewPageState state) {
        this.state = state;
    }

    public void close() {
        Stage stage = (Stage) label.getScene().getWindow();
        stage.close();
        isOpen = false;
    }
}
