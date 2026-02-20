package com.youruniversity.sms;

import backend.database.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(
                getClass().getResource("/frontend/view/login.fxml")
        );

        primaryStage.setTitle("Student Management System");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        DatabaseConnection.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}