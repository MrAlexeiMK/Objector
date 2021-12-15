package ru.mralexeimk.objector.controllers;

import com.github.sarxos.webcam.Webcam;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import ru.mralexeimk.objector.models.Objects;
import ru.mralexeimk.objector.other.WebCamState;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WebCamController implements Initializable {

    @FXML
    private Button btnStartCamera;
    @FXML
    private Button btnStopCamera;
    @FXML
    private ChoiceBox object;
    @FXML
    private Slider slider;
    @FXML
    private ComboBox<WebCamInfo> cbCameraOptions;
    @FXML
    private BorderPane bpWebCamPaneHolder;
    @FXML
    private FlowPane fpBottomPane;
    @FXML
    private ImageView imgWebCamCapturedImage;
    @FXML
    private Label queueLabel, header;

    public void setState(WebCamState state) {
        this.state = state;
        if(state == WebCamState.TRAIN) {
            cbCameraOptions.setDisable(true);
            object.setVisible(true);
            object.setDisable(false);
            for (String obj : MainController.objects.getObjects()) {
                object.getItems().add(obj);
            }
        }
        else {
            queueLabel.setVisible(false);
            slider.setVisible(false);
            header.setText("");
        }
    }

    private class WebCamInfo {

        private String webCamName;
        private int webCamIndex;

        public String getWebCamName() {
            return webCamName;
        }

        public void setWebCamName(String webCamName) {
            this.webCamName = webCamName;
        }

        public int getWebCamIndex() {
            return webCamIndex;
        }

        public void setWebCamIndex(int webCamIndex) {
            this.webCamIndex = webCamIndex;
        }

        @Override
        public String toString() {
            return webCamName;
        }
    }

    private BufferedImage grabbedImage;
    private Webcam selWebCam = null;
    private static boolean stopCamera = false;
    public static boolean isOpen = false;
    private static boolean stopTask = false;
    public WebCamState state = WebCamState.DEFAULT;
    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();

    private String cameraListPromptText = "Выбрать камеру";

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        isOpen = true;
        btnStartCamera.setDisable(true);
        btnStopCamera.setDisable(true);
        ObservableList<WebCamInfo> options = FXCollections.observableArrayList();
        int webCamCounter = 0;
        for (Webcam webcam : Webcam.getWebcams()) {
            WebCamInfo webCamInfo = new WebCamInfo();
            webCamInfo.setWebCamIndex(webCamCounter);
            webCamInfo.setWebCamName(webcam.getName());
            options.add(webCamInfo);
            webCamCounter++;
        }
        cbCameraOptions.setItems(options);
        cbCameraOptions.setPromptText(cameraListPromptText);
        cbCameraOptions.getSelectionModel().selectedItemProperty().addListener((arg01, arg11, arg2) -> {
            if (arg2 != null) {
                btnStartCamera.setDisable(false);
                btnStopCamera.setDisable(false);
                System.out.println("Индекс: " + arg2.getWebCamIndex() + ": Название камеры: " + arg2.getWebCamName());
                initializeWebCam(arg2.getWebCamIndex());
            }
        });
        Platform.runLater(() -> setImageViewSize());
    }

    public void choiceObject() {
        String obj = object.getValue().toString();
        cbCameraOptions.setDisable(false);
    }

    public void close() {
        if(state == WebCamState.TRAIN) {
            MainController.objects.saveToFiles();
        }
        closeCamera();
        stopTask = true;
        Stage stage = (Stage) btnStartCamera.getScene().getWindow();
        stage.close();
        isOpen = false;
    }

    protected void setImageViewSize() {

        double height = bpWebCamPaneHolder.getHeight();
        double width = bpWebCamPaneHolder.getWidth();
        imgWebCamCapturedImage.setFitHeight(height);
        imgWebCamCapturedImage.setFitWidth(width);
        imgWebCamCapturedImage.prefHeight(height);
        imgWebCamCapturedImage.prefWidth(width);
        imgWebCamCapturedImage.setPreserveRatio(true);

    }

    protected void initializeWebCam(final int webCamIndex) {

        Task<Void> webCamInitializer = new Task<>() {

            @Override
            protected Void call() {

                if (selWebCam != null) {
                    closeCamera();
                }
                selWebCam = Webcam.getWebcams().get(webCamIndex);
                selWebCam.open();
                startWebCamStream();
                return null;
            }

        };

        new Thread(webCamInitializer).start();
        fpBottomPane.setDisable(false);
        btnStartCamera.setDisable(true);
    }

    public void speak(String text) {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        Thread th = new Thread(() -> {
            Voice voice;
            VoiceManager vm = VoiceManager.getInstance();
            voice = vm.getVoice("kevin16");
            voice.allocate();
            voice.setRate(190);
            voice.setPitch(150);
            voice.setVolume(3);
            System.out.println(text);
            try {
                voice.speak("Hello, " + text);
                voice.deallocate();
            } catch(Exception e){
                e.printStackTrace();
            }
        });
        th.start();
    }

    protected void startWebCamStream() {
        stopCamera = false;
        stopTask = false;

        Task<Boolean> task = new Task<>() {

            @Override
            protected Boolean call() {
                BufferedImage predImage = null;
                while (!stopTask) {
                    try {
                        if ((grabbedImage = selWebCam.getImage()) != null) {
                            if(!stopCamera) {
                                BufferedImage finalPredImage = predImage;
                                grabbedImage = Objects.resize(grabbedImage, 160, 90);
                                Platform.runLater(() -> {
                                    if (finalPredImage != null && grabbedImage != null) {
                                        if (state == WebCamState.TRAIN) {
                                            grabbedImage = convert(finalPredImage, grabbedImage);
                                            List<Double> input = MainController.objects.parseImage(grabbedImage, 32, 32);
                                            MainController.objects.threadTrain(input, object.getValue().toString());
                                            queueLabel.setText("В очереди: " + MainController.objects.getQueueCount());
                                        } else if (state == WebCamState.QUERY) {
                                            List<Double> input = MainController.objects.parseImage(convert(finalPredImage, grabbedImage), 32, 32);
                                            MainController.objects.threadQuery(input);
                                            Objects.Result res = MainController.objects.getQueryResult();
                                            if (res != null) {
                                                String cur = "";
                                                if (!header.getText().equals("")) cur = header.getText().split(", ")[0];
                                                header.setText(res.getId() + ", " + Math.round(res.getProb() * 100) / 100.0);
                                                if (!cur.equals(res.getId()) && res.getProb() > 0.9 && !res.getId().equals("Nothing")) speak(res.getId());
                                            }
                                        }
                                    }
                                    if(grabbedImage != null) {
                                        Image img = SwingFXUtils
                                                .toFXImage(grabbedImage, null);
                                        imageProperty.set(img);
                                    }
                                });
                                predImage = grabbedImage;
                                grabbedImage.flush();
                            }
                            else if(state == WebCamState.TRAIN) {
                                Platform.runLater(() -> {
                                    queueLabel.setText("В очереди: " + MainController.objects.getQueueCount());
                                    if (MainController.objects.getQueueCount() == 0) {
                                        MainController.objects.saveToFiles();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        };

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        imgWebCamCapturedImage.imageProperty().bind(imageProperty);
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public BufferedImage convert(BufferedImage pred, BufferedImage cur) {
        int val = (int) slider.getValue();
        BufferedImage res = new BufferedImage(pred.getWidth(), pred.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = res.createGraphics();
        graphics.setPaint ( new Color ( 255, 255, 255 ) );
        graphics.fillRect ( 0, 0, pred.getWidth(), pred.getHeight() );
        for(int y = 0; y < res.getHeight(); ++y) {
            for(int x = 0; x < res.getWidth(); ++x) {
                int red1 = (pred.getRGB(x, y) >> 16) & 0xFF;
                int green1 = (pred.getRGB(x, y) >> 8) & 0xFF;
                int blue1 = (pred.getRGB(x, y)) & 0xFF;
                int red2 = (cur.getRGB(x, y) >> 16) & 0xFF;
                int green2 = (cur.getRGB(x, y) >> 8) & 0xFF;
                int blue2 = (cur.getRGB(x, y)) & 0xFF;
                if(Math.abs(red1-red2) >= val && Math.abs(green1-green2) >= val && Math.abs(blue1-blue2) >= val) {
                    res.setRGB(x, y, cur.getRGB(x, y));
                }
            }
        }
        return res;
    }

    private void closeCamera() {
        if (selWebCam != null) {
            selWebCam.close();
        }
    }

    public void stopCamera(ActionEvent event) {
        if(state == WebCamState.TRAIN) {
            MainController.objects.saveToFiles();
        }
        stopCamera = true;
        btnStartCamera.setDisable(false);
        btnStopCamera.setDisable(true);
    }

    public void startCamera(ActionEvent event) {
        stopCamera = false;
        startWebCamStream();
        btnStartCamera.setDisable(true);
        btnStopCamera.setDisable(false);
    }
}