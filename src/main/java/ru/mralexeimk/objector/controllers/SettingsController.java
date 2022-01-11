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
    private CheckBox rewrite, onlyMoving, detectColors, severalObjects;
    @FXML
    private TextArea configuration;
    @FXML
    private TextField trainHeight, trainWeight, queryHeight, queryWeight, lr, separation;
    @FXML
    private ChoiceBox configs;


    private boolean firstLoad = false;
    private static final List<String> configObjects =
            new ArrayList<>(List.of(
                    "Свёрточная (чёрно-белая)",
                    "Свёрточная (цветная)",
                    "Обычная (чёрно-белая)",
                    "Обычная (цветная)",
                    "Расширенная (чёрно-белая)",
                    "Расширенная (цветная)",
                    "Пользовательская"
            ));

    @FXML
    public void initialize() {
        firstLoad = true;
        configs.getItems().addAll(configObjects);
        configs.setValue(configs.getItems().get(0));
        isOpen = true;
        separation.setText(String.valueOf(SettingsListener.get().getSeparate()));
        rewrite.setSelected(SettingsListener.get().isRewriteWeights());
        onlyMoving.setSelected(SettingsListener.get().isOnlyMoving());
        configs.setValue(configObjects.get(SettingsListener.get().getExampleSelected()));
        updateConfigs();
    }

    public void updateConfigs() {
        configuration.setText(SettingsListener.get().getConfiguration());
        lr.setText(String.valueOf(SettingsListener.get().getLr()));
        detectColors.setSelected(SettingsListener.get().isDetectColors());
        severalObjects.setSelected(SettingsListener.get().isSeveralObjects());
        queryWeight.setText(String.valueOf(SettingsListener.get().getWebCamQualityQuery().getFirst()));
        queryHeight.setText(String.valueOf(SettingsListener.get().getWebCamQualityQuery().getSecond()));
        trainWeight.setText(String.valueOf(SettingsListener.get().getWebCamQualityTrain().getFirst()));
        trainHeight.setText(String.valueOf(SettingsListener.get().getWebCamQualityTrain().getSecond()));
    }

    @FXML
    public void detectColorsAction() {
        SettingsListener.get().setDetectColors(detectColors.isSelected());
        SettingsListener.get().toDefaultConfiguration();
        configuration.setText(SettingsListener.get().getConfiguration());
    }

    @FXML
    public void selectConfig() {
        if(!firstLoad) {
            String config = configs.getValue().toString();
            for (int i = 0; i < configObjects.size(); ++i) {
                if (config.equals(configObjects.get(i))) {
                    SettingsListener.get().selectConfiguration(i);
                    if(i < configObjects.size()-1) {
                        updateConfigs();
                        break;
                    }
                }
            }
        }
        else firstLoad = false;
    }

    public void close() {
        try {
            SettingsListener.get().setRewriteWeights(rewrite.isSelected());
            SettingsListener.get().setOnlyMoving(onlyMoving.isSelected());
            SettingsListener.get().setLr(Double.parseDouble(lr.getText()));
            SettingsListener.get().setDetectColors(detectColors.isSelected());
            SettingsListener.get().setSeveralObjects(severalObjects.isSelected());
            if (!SettingsListener.get().setConfiguration(configuration.getText())) {
                SettingsListener.get().toDefaultConfiguration();
            }
            SettingsListener.get().setWebCamQualityQuery(new Pair<>(Integer.parseInt(queryWeight.getText()),
                    Integer.parseInt(queryHeight.getText())));
            SettingsListener.get().setWebCamQualityTrain(new Pair<>(Integer.parseInt(trainWeight.getText()),
                    Integer.parseInt(trainHeight.getText())));
            SettingsListener.get().setSeparate(Double.parseDouble(separation.getText()));
            SettingsListener.save();
        } catch (Exception ignored) {}
        Stage stage = (Stage) rewrite.getScene().getWindow();
        stage.close();
        isOpen = false;
    }
}
