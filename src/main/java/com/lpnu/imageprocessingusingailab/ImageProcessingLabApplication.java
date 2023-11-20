package com.lpnu.imageprocessingusingailab;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ImageProcessingLabApplication extends Application {
    private static final String VIEW_FILE = "image-processing-lab-view.fxml";
    private static final String TITLE = "Обробка зображень";
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ImageProcessingLabApplication.class.getResource(VIEW_FILE));
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}