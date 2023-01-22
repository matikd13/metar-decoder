package pl.jakubowskii.metardecoder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.apache.logging.log4j.*;
import pl.jakubowskii.metardecoder.datafetcher.Metar;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    static Logger logger = LogManager.getLogger();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png")));
        stage.getIcons().add(icon);

        stage.setTitle("Metar Decoder");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) throws IOException {

        logger.info("Starting program");

//        Metar testowy = new Metar("EPKT");
//
//        logger.info(testowy.getTime());

        launch();
    }
}