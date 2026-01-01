package com.cybergame.ui.fxcontroller;

import com.cybergame.app.AppContext;
import com.cybergame.controller.AuthController;
import com.cybergame.model.entity.Computer;
import com.cybergame.model.entity.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ClientLoginController {

    // ================= FXML =================
    @FXML private TextField custUser;
    @FXML private PasswordField custPass;

    // ================= BACKEND (DÙNG CHUNG) =================
    private final AuthController authController =
            new AuthController(
                    AppContext.accountRepo,
                    AppContext.sessionManager,
                    AppContext.accountContext
            );

    // ================= PC INFO =================
    private int pcId = 1; // mặc định, MainApp sẽ set lại

    /**
     * MainApp gọi hàm này để gán PC-ID cho cửa sổ client
     */
    public void setPcId(int pcId) {
        this.pcId = pcId;
        System.out.println("Client window gán PC-ID = " + pcId);
    }

    // ================= ACTION =================

    @FXML
    private void handleCustomerLogin(ActionEvent event) {

        String username = custUser.getText().trim();
        String password = custPass.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu");
            return;
        }

        // ===== LẤY THÔNG TIN MÁY =====
        Optional<Computer> pcOpt = AppContext.computerRepo.findAll()
                .stream()
                .filter(c -> c.getComputerId() == pcId)
                .findFirst();

        if (pcOpt.isEmpty()) {
            showAlert(
                    "Lỗi hệ thống",
                    "Không tìm thấy máy trạm PC-" + pcId
            );
            return;
        }

        try {
            // ===== LOGIN BACKEND =====
            Session session = authController.loginCustomer(
                    username,
                    password,
                    pcOpt.get()
            );

            if (session == null) {
                showAlert(
                        "Đăng nhập thất bại",
                        "Sai tài khoản, mật khẩu hoặc tài khoản bị khóa"
                );
                return;
            }

            // ===== CHUYỂN SANG DASHBOARD =====
            openClientDashboard(event, session);

        } catch (IllegalStateException e) {
            // 🔥 TÀI KHOẢN ĐANG ONLINE
            showAlert("Không thể đăng nhập", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    // ================= UI HELPER =================

    private void openClientDashboard(ActionEvent event, Session session)
            throws IOException {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/client/client_dashboard.fxml"
                        )
                );

        Parent root = loader.load();

        ClientController clientController =
                loader.getController();

        // 🔥 TRUYỀN SESSION CHO DASHBOARD
        clientController.setSession(session);

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setScene(new Scene(root));
        stage.setTitle(
                "CyberGame - " +
                session.getAccount().getUsername() +
                " (PC-" + pcId + ")"
        );
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
