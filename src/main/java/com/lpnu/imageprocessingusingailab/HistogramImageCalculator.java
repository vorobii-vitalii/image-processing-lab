package com.lpnu.imageprocessingusingailab;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class HistogramImageCalculator {
    private static final int MAX_INTENSITY = 255;
    private static final int MAX_ANGLE = 360;

    public Image calculateHistogramImage(Image originalImage, Coordinate coordinate, double angle) {
        long before = System.currentTimeMillis();
        var centerX = coordinate.x();
        var centerY = coordinate.y();
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

}
