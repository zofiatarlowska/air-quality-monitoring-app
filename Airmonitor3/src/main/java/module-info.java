module Airmonitor3 {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires java.prefs;


    opens airmonitor to javafx.graphics, javafx.fxml;
    opens airmonitor.Model to com.fasterxml.jackson.databind;

    exports airmonitor;
}