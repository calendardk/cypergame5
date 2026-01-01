package com.cybergame.app;

import com.cybergame.model.entity.Computer;
import com.cybergame.repository.sql.ComputerRepositorySQL;
import com.cybergame.ui.fxcontroller.ClientLoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class MainApp extends Application {

    private final ComputerRepositorySQL computerRepo =
            new ComputerRepositorySQL();

    @Override
    public void start(Stage primaryStage) {
        try {
            // ================= SERVER =================
            openServerWindow(primaryStage);

            // ================= CLIENT =================
            List<Computer> computers = computerRepo.findAll();

            double baseX = 300;
            double baseY = 400;
            double offset = 30; // lệch nhẹ mỗi cửa sổ

            int index = 0;
            for (Computer pc : computers) {
                openClientWindow(
                        pc.getComputerId(),
                        baseX + index * offset,
                        baseY + index * offset
                );
                index++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SERVER =================

    private void openServerWindow(Stage stage) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/login/staff_login.fxml"
                        )
                );

        Parent root = loader.load();

        stage.setScene(new Scene(root));
        stage.setTitle("CyberGame - SERVER");
        stage.setX(350);
        stage.setY(50);
        stage.show();
    }

    // ================= CLIENT =================

    private void openClientWindow(int pcId, double x, double y) {
        try {
            Stage stage = new Stage();

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/login/client_login.fxml"
                            )
                    );

            Parent root = loader.load();

            ClientLoginController controller =
                    loader.getController();
            controller.setPcId(pcId);

            stage.setScene(new Scene(root));
            stage.setTitle("CLIENT - PC " + pcId);
            stage.setX(x);
            stage.setY(y);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
