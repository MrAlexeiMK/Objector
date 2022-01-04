package ru.mralexeimk.objector.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mralexeimk.objector.other.Pair;
import ru.mralexeimk.objector.singletons.SettingsListener;

public class SettingsController {
    public static boolean isOpen = false;
    @FXML
    private CheckBox rewrite, onlyMoving;
    @FXML
    private TextArea configuration;
    @FXML
    private TextField webCamWeight, webCamHeight, lr;

    @FXML
    public void initialize() {
        isOpen = true;
        rewrite.setSelected(SettingsListener.get().isRewriteWeights());
        onlyMoving.setSelected(SettingsListener.get().isOnlyMoving());
        configuration.setText(SettingsListener.get().getConfiguration());
        lr.setText(String.valueOf(SettingsListener.get().getLr()));
        webCamWeight.setText(String.valueOf(SettingsListener.get().getWebCamQuality().getFirst()));
        webCamHeight.setText(String.valueOf(SettingsListener.get().getWebCamQuality().getSecond()));
    }

    public void close() {
        try {
            SettingsListener.get().setRewriteWeights(rewrite.isSelected());
            SettingsListener.get().setOnlyMoving(onlyMoving.isSelected());
            SettingsListener.get().setLr(Double.parseDouble(lr.getText()));
            if (!SettingsListener.get().setConfiguration(configuration.getText())) {
                SettingsListener.get().toDefaultConfiguration();
            }
            SettingsListener.get().setWebCamQuality(new Pair<>(Integer.parseInt(webCamWeight.getText()),
                    Integer.parseInt(webCamHeight.getText())));
            SettingsListener.save();
        } catch (Exception ex) {}
        Stage stage = (Stage) rewrite.getScene().getWindow();
        stage.close();
        isOpen = false;
    }
}
