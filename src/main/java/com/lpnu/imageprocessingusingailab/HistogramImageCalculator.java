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
        int numSegments = (int) (MAX_ANGLE / angle);
        var histogramArray = initializeHistogramArrays(numSegments);
        performCalculation(coordinate, originalImage, numSegments, histogramArray);
        calculateCumulativeHistogram(histogramArray);
        var resultImage = projectCumulativeHistogramsOnImage(numSegments, histogramArray);
        System.out.println("OPERATION TOOK " + (System.currentTimeMillis() - before) + " ms");
        return resultImage;
    }

    private static void performCalculation(
            Coordinate coordinate,
            Image originalImage,
            int numSegments,
            AtomicInteger[][] histogramArray
    ) {
        var calculateHistogramRows =
                createCalculationTasks(originalImage, coordinate, numSegments, histogramArray);
        try {
            CompletableFuture.allOf(calculateHistogramRows).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<?>[] createCalculationTasks(
            Image originalImage,
            Coordinate coordinate,
            int numSegments,
            AtomicInteger[][] histogramArray
    ) {
        var pixelReader = originalImage.getPixelReader();
        var width = (int) originalImage.getWidth();
        var height = (int) originalImage.getHeight();
        return IntStream.range(0, width)
                .mapToObj(x -> CompletableFuture.supplyAsync(() -> {
                    for (var y = 0; y < height; y++) {
                        var pixelColor = pixelReader.getColor(x, y);

                        var r = pixelColor.getRed();
                        var g = pixelColor.getGreen();
                        var b = pixelColor.getBlue();

                        var avg = (r + g + b) / 3;
                        var intensity = (int) (avg * MAX_INTENSITY);
                        // [0; 2 PI]
                        double angleInRadians = Math.atan2(y - coordinate.y(), x - coordinate.x()) + Math.PI;
                        // [0 - 360]
                        int angleInDegrees = (int) ((angleInRadians * MAX_ANGLE) / (2 * Math.PI));
                        if (angleInDegrees == MAX_ANGLE) {
                            angleInDegrees = 0;
                        }
                        int componentNum = (int) (((double) angleInDegrees / MAX_ANGLE) * numSegments);
                        histogramArray[componentNum][intensity].incrementAndGet();
                    }
                    return x;
                }))
                .toArray(CompletableFuture[]::new);
    }

    private static WritableImage projectCumulativeHistogramsOnImage(
            int numSegments,
            AtomicInteger[][] hystogramArray
    ) {
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
        return writableImage;
    }

    private static void calculateCumulativeHistogram(AtomicInteger[][] hystogramArray) {
        for (var row : hystogramArray) {
            for (var j = 1; j < row.length; j++) {
                row[j].addAndGet(row[j - 1].get());
            }
        }
    }

    private static AtomicInteger[][] initializeHistogramArrays(int numSegments) {
        var histogramArray = new AtomicInteger[numSegments][MAX_INTENSITY + 1];
        for (int i = 0; i < numSegments; i++) {
            for (int j = 0; j < MAX_INTENSITY + 1; j++) {
                histogramArray[i][j] = new AtomicInteger(0);
            }
        }
        return histogramArray;
    }

}
