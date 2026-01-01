package com.cybergame.ui.fxcontroller;

import com.cybergame.app.AppContext;
import com.cybergame.controller.AuthController;
import com.cybergame.model.entity.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ClientController {

    // ================= FXML =================
    @FXML private Label lblUsername;
    @FXML private Label lblPcName;
    @FXML private Label lblBalance;
    @FXML private Label lblTimeUsage;

    // ================= DATA =================
    private Session currentSession;
    private Timeline usageTimer;

    // ================= BACKEND (DÙNG CHUNG) =================
    private final AuthController authController =
            new AuthController(
                    AppContext.accountRepo,
                    AppContext.sessionManager,
                    AppContext.accountContext
            );

    // ================= SET SESSION =================

    /**
     * ClientLoginController gọi hàm này
     * để truyền Session sang Dashboard
     */
    public void setSession(Session session) {
        this.currentSession = session;
        updateUI();
        startTimer();
    }

    // ================= UI UPDATE =================

    private void updateUI() {
        if (currentSession == null) return;

        lblUsername.setText(
                currentSession.getAccount().getUsername()
        );

        if (currentSession.getComputer() != null) {
            lblPcName.setText(
                    currentSession.getComputer().getName()
            );
        }

        lblBalance.setText(
                String.format(
                        "%,.0f đ",
                        currentSession.getAccount().getBalance()
                )
        );
    }

    // ================= TIMER =================

    private void startTimer() {
        if (usageTimer != null) {
            usageTimer.stop();
        }

        usageTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (currentSession == null) return;

                    LocalDateTime start =
                            currentSession.getStartTime();
                    LocalDateTime now =
                            LocalDateTime.now();

                    long seconds =
                            ChronoUnit.SECONDS
                                    .between(start, now);

                    long h = seconds / 3600;
                    long m = (seconds % 3600) / 60;
                    long s = seconds % 60;

                    lblTimeUsage.setText(
                            String.format(
                                    "%02d:%02d:%02d",
                                    h, m, s
                            )
                    );

                    // cập nhật số dư realtime
                    lblBalance.setText(
                            String.format(
                                    "%,.0f đ",
                                    currentSession
                                            .getAccount()
                                            .getBalance()
                            )
                    );
                })
        );

        usageTimer.setCycleCount(Timeline.INDEFINITE);
        usageTimer.play();
    }

    // ================= ACTION =================

    @FXML
    private void openServiceMenu() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Chức năng gọi món đang phát triển");
        alert.show();
    }

    @FXML
    private void handleLogout(ActionEvent event) {

        if (currentSession == null) return;

        // ===== BACKEND LOGOUT =====
        authController.logout(currentSession);

        // ===== STOP TIMER =====
        if (usageTimer != null) {
            usageTimer.stop();
        }

        // ===== QUAY VỀ MÀN LOGIN =====
        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/login/client_login.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("CLIENT LOGIN");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", e.getMessage());
        }
    }

    // ================= HELPER =================

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
