package com.lpnu.imageprocessingusingailab;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class ImageProcessingController {
    public static final int MAX_INTENSITY = 255;
    public static final int MAX_ANGLE = 360;
    private final FileChooser fileChooser;
    public Label originalImageLabel;
    public Label resultImageLabel;
    public Slider angleInputSlider;
    public Button saveResultButton;

    public ImageProcessingController() {
        this.fileChooser = new FileChooser();
        var extensionFilters = fileChooser.getExtensionFilters();
        extensionFilters.add(new FileChooser.ExtensionFilter(
                "Image Files",
                "*.jpg", "*.jpeg", "*.png", "*.tif", "*.bmp", "*.webp"
        ));
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
            System.out.println("Opened file " + openedFile);
            var loadedImage = new Image(openedFile.toURI().toString());
            originalImage.setImage(loadedImage);
            originalImageLabel.setVisible(true);
            double imageWidth = loadedImage.getWidth();
            double imageHeight = loadedImage.getHeight();
            System.out.println("Image size = " + imageWidth + "x" + imageHeight);
            originalImage.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
                System.out.println("Image actual size = " + originalImage.getBoundsInLocal().getWidth() + " " + originalImage.getBoundsInLocal().getHeight());
                System.out.println("Mouse click event " + event.getX() + " " + event.getY());
                var originalImageCoordinate = calculateOriginalImageCoordinate(originalImage, event.getX(), event.getY());
                System.out.println("Original coordinate = " + Arrays.toString(originalImageCoordinate));
                var angle = angleInputSlider.getValue();
                var resImage = calculate(originalImageCoordinate, angle);
                resultImage.setImage(resImage);
//                resultImage.setFitWidth(originalImage.getBoundsInLocal().getWidth());
//                resultImage.setFitHeight(originalImage.getBoundsInLocal().getHeight());
                resultImage.setPreserveRatio(true);
                resultImageLabel.setText("Результат при крок градусі = " + angle + ", координата x = " + originalImageCoordinate[0] + " y = " + originalImageCoordinate[1]);
                resultImageLabel.setVisible(true);
                saveResultButton.setVisible(true);
            });
            angleInputSlider.setVisible(true);
        }
    }

    private Image calculate(int[] centerCoordinate, double angle) {
        long before = System.currentTimeMillis();
        var centerX = centerCoordinate[0];
        var centerY = centerCoordinate[1];
        var originalImage = this.originalImage.getImage();
        var pixelReader = originalImage.getPixelReader();
        var width = (int) originalImage.getWidth();
        var height = (int) originalImage.getHeight();

        int numSegments = (int) (360 / angle);
        var hystogramArray = new AtomicInteger[numSegments][MAX_INTENSITY + 1];
        for (int i = 0; i < numSegments; i++) {
            for (int j = 0; j < MAX_INTENSITY + 1; j++) {
                hystogramArray[i][j] = new AtomicInteger(0);
            }
        }
        var calculateHystogramRows = IntStream.range(0, width)
                .mapToObj(x -> CompletableFuture.supplyAsync(() -> {
                    for (var y = 0; y < height; y++) {
                        var pixelColor = pixelReader.getColor(x, y);

                        var r = pixelColor.getRed();
                        var g = pixelColor.getGreen();
                        var b = pixelColor.getBlue();

                        var avg = (r + g + b) / 3;
                        var intensity = (int) (avg * MAX_INTENSITY);
                        // [0; 2 PI]
                        double angleInRadians = Math.atan2(y - centerY, x - centerX) + Math.PI;
                        // [0 - 360]
                        int angleInDegrees = (int) ((angleInRadians * MAX_ANGLE) / (2 * Math.PI));
                        if (angleInDegrees == MAX_ANGLE) {
                            angleInDegrees = 0;
                        }
                        int componentNum = (int) (((double) angleInDegrees / MAX_ANGLE) * numSegments);
                        hystogramArray[componentNum][intensity].incrementAndGet();
                    }
                    return x;
                }))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(calculateHystogramRows).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        for (var row : hystogramArray) {
            for (var j = 1; j < row.length; j++) {
                row[j].addAndGet(row[j - 1].get());
            }
        }
        var writableImage = new WritableImage(numSegments, 255);
        var pixelWriter = writableImage.getPixelWriter();
        for (int i = 0; i < numSegments; i++) {
            for (int j = 0; j < 255; j++) {
                AtomicInteger val = hystogramArray[i][j];
                AtomicInteger total = hystogramArray[i][hystogramArray[i].length - 1];
                var c = ((double) val.get() / total.get());
                pixelWriter.setColor(i, j, Color.color(c, c, c));
            }
        }
        System.out.println("OPERATION TOOK " + (System.currentTimeMillis() - before) + " ms");
        return writableImage;
    }

    private Image resample(Image input, int scaleFactor) {
        final int W = (int) input.getWidth();
        final int H = (int) input.getHeight();
        final int S = scaleFactor;

        WritableImage output = new WritableImage(
                W * S,
                H * S
        );

        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                final int argb = reader.getArgb(x, y);
                for (int dy = 0; dy < S; dy++) {
                    for (int dx = 0; dx < S; dx++) {
                        writer.setArgb(x * S + dx, y * S + dy, argb);
                    }
                }
            }
        }

        return output;
    }

    // x, y
    private int[] calculateOriginalImageCoordinate(ImageView imageView, double x, double y) {
        var image = imageView.getImage();
        var bounds = imageView.getBoundsInLocal();
        var proportionHeight = y / bounds.getHeight();
        var proportionWidth = x / bounds.getWidth();
        return new int[] {
                (int) Math.floor(proportionWidth * image.getWidth()),
                (int) Math.floor(proportionHeight * image.getHeight())
        };
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

}
