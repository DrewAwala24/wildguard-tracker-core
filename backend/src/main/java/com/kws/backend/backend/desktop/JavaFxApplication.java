package com.kws.backend.backend.desktop;

import com.kws.backend.backend.BackendApplication;
import com.kws.backend.backend.desktop.controller.AuthDialogController;
import com.kws.backend.backend.desktop.controller.MainOperationsController;
import com.kws.backend.backend.model.UserAccount;
import com.kws.backend.backend.service.UserService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private MainOperationsController mainController;

    @Override
    public void init() {
        log.info("[KWS WildGuard] Bootstrapping Spring Boot & PostGIS application context...");
        springContext = new SpringApplicationBuilder(BackendApplication.class)
                .headless(false)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            log.info("[KWS WildGuard] Starting Native JavaFX Operations Desktop Stage...");

            UserService userService = springContext.getBean(UserService.class);

            // If not yet authenticated, show the authentication modal
            if (userService.getCurrentUser() == null) {
                AuthDialogController authDialog = new AuthDialogController(userService);
                UserAccount user = authDialog.showLoginDialog(primaryStage);
                if (user == null) {
                    log.info("[KWS WildGuard] Authentication cancelled. Shutting down.");
                    Platform.exit();
                    return;
                }
            }

            mainController = springContext.getBean(MainOperationsController.class);
            mainController.initialize(primaryStage);

            primaryStage.show();
            log.info("[KWS WildGuard] Operations Command Desktop Window is now LIVE on display.");
        } catch (Exception e) {
            log.error("[KWS WildGuard] Startup error in JavaFX Application: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void stop() {
        log.info("[KWS WildGuard] Gracefully closing desktop application context...");
        if (mainController != null) {
            mainController.shutdown();
        }
        if (springContext != null && springContext.isActive()) {
            springContext.close();
        }
        Platform.exit();
    }
}
