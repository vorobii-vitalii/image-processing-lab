package com.lpnu.imageprocessingusingailab;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class ImageProcessingController {
    private final FileChooser fileChooser;
    private final OriginalImageCoordinateProvider originalImageCoordinateProvider =
            new OriginalImageCoordinateProvider();
    private final HistogramImageCalculator histogramImageCalculator = new HistogramImageCalculator();
    public Label originalImageLabel;
    public Label resultImageLabel;
    public Slider angleInputSlider;
    public Button saveResultButton;

    public ImageProcessingController() {
        this.fileChooser = new FileChooser();
        var extensionFilters = fileChooser.getExtensionFilters();
        var imageChooserFactory = new ImageChooserFactory();
        extensionFilters.add(imageChooserFactory.getImageChooser());
    }

    @FXML
    private ImageView originalImage;

    @FXML
    private ImageView resultImage;

    @FXML
    protected void onLoadImageButtonClick() {
        var openedFile = fileChooser.showOpenDialog(null);
        if (openedFile == null) {
            System.out.println("No file was loaded!");
        }
        else {
            resultImageLabel.setVisible(false);
            saveResultButton.setVisible(false);
            resultImage.setImage(null);
            var loadedImage = new Image(openedFile.toURI().toString());
            originalImage.setImage(loadedImage);
            originalImageLabel.setVisible(true);
            originalImage.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                var originalImageCoordinate =
                        originalImageCoordinateProvider.calculateOriginalImageCoordinate(originalImage, event.getX(), event.getY());
                var angle = angleInputSlider.getValue();
                var resImage = histogramImageCalculator.calculateHistogramImage(originalImage.getImage(), originalImageCoordinate, angle);
                resultImage.setImage(resImage);
                resultImage.setPreserveRatio(true);
                resultImageLabel.setText(createResultImageLabel(angle, originalImageCoordinate));
                resultImageLabel.setVisible(true);
                saveResultButton.setVisible(true);
            });
            angleInputSlider.setVisible(true);
        }
    }

    @FXML
    protected void onAngleChange() {
        System.out.println("VALUE CHANGED = " + angleInputSlider.getValue());
    }

    @FXML
    protected void onResultSave() throws IOException {
        File fileToSave = fileChooser.showSaveDialog(null);
        if (fileToSave == null) {
            return;
        }
        Image image = resultImage.getImage();
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", fileToSave);
    }

    private String createResultImageLabel(double angle, Coordinate originalImageCoordinate) {
        return "Результат при крок градусі = " + angle + ", координата x = " + originalImageCoordinate.x() + " y = " + originalImageCoordinate.y();
    }

}
