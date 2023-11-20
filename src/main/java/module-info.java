module com.lpnu.imageprocessingusingailab {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.lpnu.imageprocessingusingailab to javafx.fxml;
    exports com.lpnu.imageprocessingusingailab;
}