package ru.mralexeimk.objector.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mralexeimk.objector.other.Pair;
import ru.mralexeimk.objector.singletons.SettingsListener;

import java.util.ArrayList;
import java.util.List;

public class SettingsController {
    public static boolean isOpen = false;
    @FXML
    private CheckBox rewrite, onlyMoving, detectColors;
    @FXML
    private TextArea configuration;
    @FXML
    private TextField webCamWeight, webCamHeight, lr;
    @FXML
    private ChoiceBox configs;

    private static final List<String> configObjects =
            new ArrayList<>(List.of(
                    "Свёрточная (чёрно-белая)",
                    "Свёрточная (цветная)",
                    "Обычная (чёрно-белая)",
                    "Обычная (цветная)",
                    "Расширенная (чёрно-белая)",
                    "Расширенная (цветная)"
            ));

    @FXML
    public void initialize() {
        configs.getItems().addAll(configObjects);
        configs.setValue(configs.getItems().get(0));
        isOpen = true;
        rewrite.setSelected(SettingsListener.get().isRewriteWeights());
        onlyMoving.setSelected(SettingsListener.get().isOnlyMoving());
        detectColors.setSelected(SettingsListener.get().isDetectColors());
        configuration.setText(SettingsListener.get().getConfiguration());
        lr.setText(String.valueOf(SettingsListener.get().getLr()));
        webCamWeight.setText(String.valueOf(SettingsListener.get().getWebCamQuality().getFirst()));
        webCamHeight.setText(String.valueOf(SettingsListener.get().getWebCamQuality().getSecond()));
    }

    @FXML
    public void detectColorsAction() {
        SettingsListener.get().setDetectColors(detectColors.isSelected());
        SettingsListener.get().toDefaultConfiguration();
        configuration.setText(SettingsListener.get().getConfiguration());
    }

    @FXML
    public void selectConfig() {
        String config = configs.getValue().toString();
        for(int i = 0; i < configObjects.size(); ++i) {
            if(config.equals(configObjects.get(i))) {
                SettingsListener.get().selectConfiguration(i);
                configuration.setText(SettingsListener.get().getConfiguration());
                detectColors.setSelected(SettingsListener.get().isDetectColors());
                lr.setText(String.valueOf(SettingsListener.get().getLr()));
                webCamWeight.setText(String.valueOf(SettingsListener.get().getWebCamQuality().getFirst()));
                webCamHeight.setText(String.valueOf(SettingsListener.get().getWebCamQuality().getSecond()));
                break;
            }
        }
    }

    public void close() {
        try {
            SettingsListener.get().setRewriteWeights(rewrite.isSelected());
            SettingsListener.get().setOnlyMoving(onlyMoving.isSelected());
            SettingsListener.get().setLr(Double.parseDouble(lr.getText()));
            SettingsListener.get().setDetectColors(detectColors.isSelected());
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
