module ru.mralexeimk.objector {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires javafx.graphics;
    requires java.desktop;
    requires webcam.capture;
    requires javafx.swing;
    requires freetts;
    requires lombok;

    opens ru.mralexeimk.objector.controllers to javafx.fxml;
    exports ru.mralexeimk.objector;
    exports ru.mralexeimk.objector.applications;
    exports ru.mralexeimk.objector.other;
}