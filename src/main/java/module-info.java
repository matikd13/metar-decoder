module pl.jakubowskii.metardecoder {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires org.jsoup;


    opens pl.jakubowskii.metardecoder to javafx.fxml;
    exports pl.jakubowskii.metardecoder;
}