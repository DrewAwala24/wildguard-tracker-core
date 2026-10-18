package com.kws.backend.backend.desktop.controller;

import com.kws.backend.backend.model.UserAccount;
import com.kws.backend.backend.service.UserService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AuthDialogController {

    private final UserService userService;
    private UserAccount authenticatedUser = null;

    public AuthDialogController(UserService userService) {
        this.userService = userService;
    }

    public UserAccount showLoginDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: #0B0F17; -fx-border-color: #38BDF8; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        // Header
        Label title = new Label("🦁 KENYA WILDLIFE SERVICE");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFD700;");

        Label subtitle = new Label("WildGuard Tracker — Operations Command Authentication");
        subtitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        // Pre-configured Duty Station Accounts Quick Selector
        Label quickLabel = new Label("Select Duty Post / Role:");
        quickLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #CBD5E1;");

        ComboBox<String> accountCombo = new ComboBox<>();
        accountCombo.getItems().addAll(
                "commander (Snr. Warden Peter Mwangi — OPERATIONS_COMMANDER)",
                "admin (Lead Systems Engineer Drew Awala — ADMINISTRATOR)",
                "dispatcher (Duty Officer Faith Cherono — DISPATCHER)",
                "ranger (Field Ranger Joseph Kiprono — RANGER)",
                "analyst (Wildlife Biologist Dr. Grace Njeri — ANALYST)"
        );
        accountCombo.getSelectionModel().selectFirst();
        accountCombo.setMaxWidth(Double.MAX_VALUE);
        accountCombo.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white; -fx-font-size: 11px;");

        // Username
        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #CBD5E1;");
        TextField usernameField = new TextField("commander");
        usernameField.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 4px;");

        // Password
        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #CBD5E1;");
        PasswordField passwordField = new PasswordField();
        passwordField.setText("kws123");
        passwordField.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 4px;");

        accountCombo.setOnAction(e -> {
            String selected = accountCombo.getValue();
            if (selected != null) {
                String u = selected.split(" ")[0];
                usernameField.setText(u);
                passwordField.setText("kws123");
            }
        });

        // Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Exit");
        cancelBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            dialog.close();
            System.exit(0);
        });

        Button loginBtn = new Button("🔐 Authorize & Enter Command");
        loginBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;");
        loginBtn.setDefaultButton(true);

        loginBtn.setOnAction(e -> {
            try {
                String u = usernameField.getText().trim();
                String p = passwordField.getText();
                authenticatedUser = userService.authenticate(u, p);
                dialog.close();
            } catch (Exception ex) {
                errorLabel.setText("❌ " + ex.getMessage());
                errorLabel.setVisible(true);
            }
        });

        buttonBar.getChildren().addAll(cancelBtn, loginBtn);

        root.getChildren().addAll(title, subtitle, errorLabel, quickLabel, accountCombo, userLabel, usernameField, passLabel, passwordField, buttonBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();

        return authenticatedUser;
    }
}
