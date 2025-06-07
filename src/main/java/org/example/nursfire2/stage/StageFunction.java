package org.example.nursfire2.stage;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Screen;

import java.io.IOException;
import java.util.Objects;

public class StageFunction{
    public void openMainApplication(ActionEvent event,String res,String name,boolean max) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(res));
        Scene scene = new Scene(fxmlLoader.load(),400, 300);
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        javafx.stage.Stage mainStage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
          scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(PageName.LOGINCSS_STRING)).toExternalForm());
        mainStage.setTitle(name);
        mainStage.setScene(scene);
        mainStage.setX(bounds.getMinX());
        mainStage.setY(bounds.getMinY());
        mainStage.setWidth(bounds.getWidth());
        mainStage.setHeight(bounds.getHeight());
        mainStage.setMaximized(true);
        mainStage.show();
    }


}
