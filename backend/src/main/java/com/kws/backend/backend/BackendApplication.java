package com.kws.backend.backend;

import com.kws.backend.backend.desktop.JavaFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        boolean isDisplayPresent = (System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null)
                && !"true".equalsIgnoreCase(System.getProperty("java.awt.headless"));

        if (isDisplayPresent) {
            // Launch as native single-JVM JavaFX Desktop Application with Spring Boot
            Application.launch(JavaFxApplication.class, args);
        } else {
            // Headless / Server / CI mode
            new SpringApplicationBuilder(BackendApplication.class)
                    .headless(true)
                    .run(args);
        }
    }
}
