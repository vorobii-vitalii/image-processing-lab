package com.lpnu.imageprocessingusingailab;

import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class ImageChooserFactory {
    private static final String IMAGES_CATEGORY = "Image Files";
    private static final String[] IMAGE_EXTENSIONS = new String[]{
            "*.jpg", "*.jpeg", "*.png", "*.tif", "*.bmp", "*.webp"
    };

    public FileChooser.ExtensionFilter getImageChooser() {
        return new ExtensionFilter(IMAGES_CATEGORY, IMAGE_EXTENSIONS);
    }
    
}
