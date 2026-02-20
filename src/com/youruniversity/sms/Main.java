package com.youruniversity.sms;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Login page দিয়ে শুরু করতে চাইলে:
        Parent root = FXMLLoader.load(
                getClass().getResource("/frontend/view/login.fxml")
        );

        // Registration page দিয়ে শুরু করতে চাইলে:
        // Parent root = FXMLLoader.load(
        //     getClass().getResource("/frontend/view/registerForm.fxml")
        // );

        primaryStage.setTitle("Student Management System");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}