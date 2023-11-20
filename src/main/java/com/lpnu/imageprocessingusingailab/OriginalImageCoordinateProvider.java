package com.lpnu.imageprocessingusingailab;

import javafx.scene.image.ImageView;

public class OriginalImageCoordinateProvider {

    public Coordinate calculateOriginalImageCoordinate(ImageView imageView, double x, double y) {
        var image = imageView.getImage();
        var bounds = imageView.getBoundsInLocal();
        var proportionHeight = y / bounds.getHeight();
        var proportionWidth = x / bounds.getWidth();
        return new Coordinate(
                (int) Math.floor(proportionWidth * image.getWidth()),
                (int) Math.floor(proportionHeight * image.getHeight()));
    }

}
