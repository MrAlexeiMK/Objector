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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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
import java.util.*;
import java.util.List;

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

    private BufferedImage grabbedImage;
    private Webcam selWebCam = null;
    private static boolean stopCamera = false;
    public static boolean isOpen = false;
    private static boolean stopTask = false;
    public WebCamState state = WebCamState.DEFAULT;
    private final ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
    private boolean isSeveral = false;
    private Pair<Double, Double> lastPoint;
    private Map<javafx.scene.paint.Color, String> objectByColor;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        isOpen = true;
        lastPoint = new Pair<>(0.0, 0.0);
        objectByColor = new HashMap<>();
        isSeveral = SettingsListener.get().isSeveralObjects();
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
        String cameraListPromptText = "Выбрать камеру";
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

    public javafx.scene.paint.Color convertStringToColor(String str) {
        double num1 = (double) Math.abs(str.hashCode())%500;
        double num2 = (double) Math.abs(str.hashCode())%700;
        double num3 = (double) Math.abs(str.hashCode())%900;
        return javafx.scene.paint.Color.color(num1/500, num2/700, num3/900);
    }

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
                objectByColor.put(convertStringToColor(obj), obj);
            }
            if(isSeveral) {
                start.setText("Обучить");
                btnStopCamera.setText("Сделать кадр");
                cbCameraOptions.setDisable(false);
                object.setDisable(true);
            }
        }
        else {
            queueLabel.setVisible(false);
            header.setText("");
        }
        if(isSeveral) {
            header.setText("");
            header.setVisible(false);
        }
    }

    public void close() {
        if(state == WebCamState.TRAIN) {
            NeuralNetworkListener.save();
        }
        closeCamera();
        stopTask = true;
        NeuralNetworkListener.setQueueCount(0);
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

    @FXML
    public void choiceObject() {
        if(object.getValue() != null) {
            cbCameraOptions.setDisable(false);
            if (isSeveral) {
                addRectangle(grabbedImage.getWidth() / 2, grabbedImage.getHeight() / 2,
                        90, 90, object.getValue().toString());
                object.setValue(null);
            }
        }
    }

    public void startTrain() {
        error.setText("");
        if(trainingData == null || trainingData.isEmpty()) {
            error.setText("Нет данных для обучения");
            NeuralNetworkListener.setQueueCount(0);
            return;
        }
        if(isSeveral) {
            startCamera();
        }
        List<Pair<List<List<Double>>, String>> copyTrainingData = new ArrayList<>(trainingData);
        Collections.shuffle(copyTrainingData);
        for(Pair<List<List<Double>>, String> row : copyTrainingData) {
            NeuralNetworkListener.threadTrain(row.getFirst(), row.getSecond());
        }
        trainingData.clear();
    }

    public void train() {
        List<List<Double>> input = NeuralNetworkListener.parseImage(grabbedImage,
                NeuralNetworkListener.get().getInputLayer(0).getSize());
        trainingData.add(new Pair<>(input, object.getValue().toString()));
        NeuralNetworkListener.increaseQueueCount();
        queueLabel.setText("В очереди: " + NeuralNetworkListener.getQueueCount());
    }

    public void trainSeveral() {
        for(Node node : bpWebCamPaneHolder.getChildren()) {
            if(node instanceof javafx.scene.shape.Rectangle rect) {
                BufferedImage buffered = NeuralNetworkListener.cropImage(grabbedImage, (int)rect.getLayoutX(), (int)rect.getLayoutY(),
                        (int)rect.getWidth(), (int)rect.getHeight());
                List<List<Double>> input = NeuralNetworkListener.parseImage(buffered, NeuralNetworkListener.get().getInputLayer(0).getSize());
                if(objectByColor.containsKey((javafx.scene.paint.Color) rect.getStroke())) {
                    trainingData.add(new Pair<>(input, objectByColor.get((javafx.scene.paint.Color) rect.getStroke())));
                }
                NeuralNetworkListener.increaseQueueCount();
                queueLabel.setText("В очереди: " + NeuralNetworkListener.getQueueCount());
            }
        }
    }

    public void query() {
        List<List<Double>> input = NeuralNetworkListener.parseImage(grabbedImage,
                NeuralNetworkListener.get().getInputLayer(0).getSize());
        NeuralNetworkListener.threadQuery(input);
        Pair<String, Double> res = NeuralNetworkListener.getQueryResult();
        if (res != null) {
            header.setText(res.getFirst() + ", " + Math.round(res.getSecond() * 100) / 100.0);
        }
    }

    public void querySeveral() {
        NeuralNetworkListener.threadQuerySeveralObjects(grabbedImage,
                SettingsListener.get().getWebCamQualityTrain().getSecond());
        bpWebCamPaneHolder.getChildren().removeIf(node -> node instanceof javafx.scene.shape.Rectangle);
        for(Rectangle rec : NeuralNetworkListener.getRectangles()) {
            Text text = new Text(rec.getQueryRes().getFirst()+", "+
                    Math.round(rec.getQueryRes().getSecond()*100)/100.0);
            text.setX(rec.getX());
            text.setY(rec.getY()-10);
            text.setFont(Font.font(10));
            addRectangle(rec.getX(), rec.getY(), rec.getLenX(), rec.getLenY(), rec.getQueryRes().getFirst());
        }
    }

    public void addRectangle(int x, int y, int lenX, int lenY, String object) {
        javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle(
                0, 0,
                lenX, lenY);
        rectangle.setLayoutX(x);
        rectangle.setLayoutY(y);
        rectangle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        rectangle.setStroke(convertStringToColor(object));
        rectangle.setOnMousePressed(event -> {
            lastPoint.setFirst(rectangle.getLayoutX() - event.getSceneX());
            lastPoint.setSecond(rectangle.getLayoutY() - event.getSceneY());
            rectangle.setCursor(Cursor.MOVE);
        });
        rectangle.setOnMouseReleased(event -> {
            rectangle.setCursor(Cursor.HAND);
        });
        rectangle.setOnMouseDragged(event -> {
            rectangle.setLayoutX(event.getSceneX() + lastPoint.getFirst());
            rectangle.setLayoutY(event.getSceneY() + lastPoint.getSecond());
        });
        rectangle.setOnMouseEntered(event -> {
            rectangle.setCursor(Cursor.HAND);
        });
        rectangle.setOnScroll(event -> {
            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();
            if (deltaY < 0){
                zoomFactor = 2.0 - zoomFactor;
            }
            rectangle.setScaleX(rectangle.getScaleX() * zoomFactor);
            rectangle.setScaleY(rectangle.getScaleY() * zoomFactor);
            event.consume();
        });
        bpWebCamPaneHolder.getChildren().add(rectangle);
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
                                Platform.runLater(() -> {
                                    if (finalPredImage != null && grabbedImage != null) {
                                        if(SettingsListener.get().isOnlyMoving()) grabbedImage = convert(finalPredImage, grabbedImage);
                                        if (state == WebCamState.TRAIN) {
                                            if(!isSeveral) {
                                                train();
                                            }
                                        } else if (state == WebCamState.QUERY) {
                                            if(isSeveral) {
                                                querySeveral();
                                            }
                                            else {
                                                query();
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

    public void convertGrabbedImage() {
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

    public void stopCamera() {
        if(state == WebCamState.TRAIN) {
            if (btnStopCamera.getText().equals("Сделать кадр")) {
                object.setDisable(false);
                trainSeveral();
            }
        }
        stopCamera = true;
        btnStartCamera.setDisable(false);
        btnStopCamera.setDisable(true);
    }

    public void startCamera() {
        stopCamera = false;
        startWebCamStream();
        btnStartCamera.setDisable(true);
        btnStopCamera.setDisable(false);
    }

    private static class WebCamInfo {

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
}