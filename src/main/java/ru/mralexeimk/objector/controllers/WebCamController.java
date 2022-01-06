package ru.mralexeimk.objector.controllers;

import com.github.sarxos.webcam.Webcam;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import eu.hansolo.tilesfx.colors.ColorSkin;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import ru.mralexeimk.objector.other.Pair;
import ru.mralexeimk.objector.singletons.NeuralNetworkListener;
import ru.mralexeimk.objector.other.WebCamState;
import ru.mralexeimk.objector.singletons.SettingsListener;
import ru.mralexeimk.objector.other.Rectangle;

import javax.swing.plaf.synth.ColorType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class WebCamController implements Initializable {

    @FXML
    private Button btnStartCamera;
    @FXML
    private Button btnStopCamera;
    @FXML
    private Button start;
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
    private Label queueLabel, header, error;

    private List<Pair<List<List<Double>>, String>> trainingData;

    public void setState(WebCamState state) {
        this.state = state;
        if(!SettingsListener.get().isOnlyMoving()) {
            slider.setDisable(true);
            slider.setVisible(false);
        }
        if(state == WebCamState.TRAIN) {
            cbCameraOptions.setDisable(true);
            start.setVisible(true);
            start.setDisable(false);
            object.setVisible(true);
            object.setDisable(false);
            for (String obj : NeuralNetworkListener.get().getObjects()) {
                object.getItems().add(obj);
            }
        }
        else {
            queueLabel.setVisible(false);
            header.setText("");
        }
        if(SettingsListener.get().isSeveralObjects()) {
            header.setText("");
            header.setVisible(false);
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
        trainingData = new ArrayList<>();
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
        Platform.runLater(this::setImageViewSize);
    }

    public void choiceObject() {
        cbCameraOptions.setDisable(false);
    }

    public void startTrain() {
        error.setText("");
        if(trainingData == null || trainingData.isEmpty()) {
            error.setText("Нет данных для обучения");
            return;
        }
        List<Pair<List<List<Double>>, String>> copyTrainingData = new ArrayList<>(trainingData);
        Collections.shuffle(copyTrainingData);
        for(Pair<List<List<Double>>, String> row : copyTrainingData) {
            NeuralNetworkListener.threadTrain(row.getFirst(), row.getSecond());
        }
        trainingData.clear();
    }

    public void close() {
        if(state == WebCamState.TRAIN) {
            NeuralNetworkListener.save();
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
                                if(state == WebCamState.TRAIN) {
                                    double K = (double)SettingsListener.get().getWebCamQualityQuery().getFirst()
                                            /SettingsListener.get().getWebCamQualityQuery().getSecond();
                                    int guessWidth = (int) (SettingsListener.get().getWebCamQualityTrain().getSecond()*K);
                                    int width = SettingsListener.get().getWebCamQualityTrain().getFirst();
                                    int height = SettingsListener.get().getWebCamQualityTrain().getSecond();
                                    grabbedImage = NeuralNetworkListener.resize(grabbedImage,
                                            guessWidth,
                                            height);
                                    if(guessWidth > width) {
                                        int crop = (guessWidth-width)/2;
                                        grabbedImage = NeuralNetworkListener.cropImage(grabbedImage, crop, 0, width, height);
                                    }
                                    grabbedImage = NeuralNetworkListener.resize(grabbedImage,
                                            width,
                                            height);
                                }
                                else if(state == WebCamState.QUERY) {
                                    grabbedImage = NeuralNetworkListener.resize(grabbedImage,
                                            SettingsListener.get().getWebCamQualityQuery().getFirst(),
                                            SettingsListener.get().getWebCamQualityQuery().getSecond());
                                }
                                Platform.runLater(() -> {
                                    if (finalPredImage != null && grabbedImage != null) {
                                        if(SettingsListener.get().isOnlyMoving()) grabbedImage = convert(finalPredImage, grabbedImage);
                                        if (state == WebCamState.TRAIN) {
                                            List<List<Double>> input = NeuralNetworkListener.parseImage(grabbedImage,
                                                    NeuralNetworkListener.get().getInputLayer(0).getSize());
                                            trainingData.add(new Pair<>(input, object.getValue().toString()));
                                            NeuralNetworkListener.increaseQueueCount();
                                            queueLabel.setText("В очереди: " + NeuralNetworkListener.getQueueCount());
                                        } else if (state == WebCamState.QUERY) {
                                            if(SettingsListener.get().isSeveralObjects()) {
                                                NeuralNetworkListener.threadQuerySeveralObjects(grabbedImage,
                                                        SettingsListener.get().getWebCamQualityTrain().getSecond());
                                                bpWebCamPaneHolder.getChildren().removeIf(node -> node instanceof Text);
                                                bpWebCamPaneHolder.getChildren().removeIf(node -> node instanceof javafx.scene.shape.Rectangle);
                                                for(Rectangle rec : NeuralNetworkListener.getRectangles()) {
                                                    javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle(
                                                            rec.getX(), rec.getY(),
                                                            rec.getLenX(), rec.getLenY());
                                                    rectangle.setFill(javafx.scene.paint.Color.TRANSPARENT);
                                                    rectangle.setStroke(javafx.scene.paint.Color.BLACK);
                                                    Text text = new Text(rec.getQueryRes().getFirst()+", "+
                                                            Math.round(rec.getQueryRes().getSecond()*100)/100.0);
                                                    text.setX(rec.getX());
                                                    text.setY(rec.getY()-10);
                                                    text.setFont(Font.font(10));
                                                    bpWebCamPaneHolder.getChildren().add(rectangle);
                                                    bpWebCamPaneHolder.getChildren().add(text);
                                                }
                                            }
                                            else {
                                                List<List<Double>> input = NeuralNetworkListener.parseImage(grabbedImage,
                                                        NeuralNetworkListener.get().getInputLayer(0).getSize());
                                                NeuralNetworkListener.threadQuery(input);
                                                Pair<String, Double> res = NeuralNetworkListener.getQueryResult();
                                                if (res != null) {
                                                    header.setText(res.getFirst() + ", " + Math.round(res.getSecond() * 100) / 100.0);
                                                }
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
                                    queueLabel.setText("В очереди: " + NeuralNetworkListener.getQueueCount());
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