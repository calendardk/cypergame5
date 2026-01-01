package com.cybergame.ui.fxcontroller;

import com.cybergame.app.AppContext;
import com.cybergame.controller.AuthController;
import com.cybergame.controller.EmployeeAuthController;
import com.cybergame.model.entity.Computer;
import com.cybergame.model.entity.Employee;
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

public class LoginController {

    // =======================================================
    // BACKEND (DÙNG CHUNG – QUA AppContext)
    // =======================================================

    // --- STAFF / EMPLOYEE (GIỮ NGUYÊN) ---
    private final EmployeeAuthController empAuth =
            new EmployeeAuthController(AppContext.employeeRepo);

    // --- CLIENT (ĐÃ SỬA THEO AUTH + SESSION MỚI) ---
    private final AuthController clientAuth =
            new AuthController(
                    AppContext.accountRepo,
                    AppContext.sessionManager,
                    AppContext.accountContext
            );

    // =======================================================
    // FXML FIELDS
    // =======================================================
    @FXML private TextField txtStaffUser;
    @FXML private PasswordField txtStaffPass;
    @FXML private PasswordField txtAdminPin;
    @FXML private TextField custUser;
    @FXML private PasswordField custPass;

    // =======================================================
    // 1. ĐĂNG NHẬP NHÂN VIÊN (KHÔNG SỬA)
    // =======================================================
    @FXML
    private void handleStaffLogin(ActionEvent event) {

        String user = txtStaffUser.getText();
        String pass = txtStaffPass.getText();

        if (user.isBlank() || pass.isBlank()) {
            showAlert("Lỗi", "Vui lòng nhập tài khoản và mật khẩu!");
            return;
        }

        Employee emp = empAuth.login(user, pass);

        if (emp == null) {
            showAlert("Đăng nhập thất bại",
                    "Sai thông tin hoặc tài khoản đã bị khóa!");
            return;
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(
                            "/fxml/staff/staff_dashboard.fxml"));

            Parent root = loader.load();

            StaffDashboardController ctrl =
                    loader.getController();
            ctrl.setStaffInfo(emp);

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("CyberGame - Staff Dashboard");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", e.getMessage());
        }
    }

    // =======================================================
    // 2. ĐĂNG NHẬP ADMIN (KHÔNG SỬA)
    // =======================================================
    @FXML
    private void handleAdminLogin(ActionEvent event) {

        String pin = txtAdminPin.getText();

        if (!"9999".equals(pin) && !"admin".equalsIgnoreCase(pin)) {
            showAlert("Truy cập bị từ chối",
                    "Mã PIN xác thực không đúng!");
            return;
        }

        navigate(event,
                "/fxml/admin/admin_dashboard.fxml",
                "CyberGame - Administrator");
    }

    // =======================================================
    // 3. ĐĂNG NHẬP KHÁCH HÀNG (ĐÃ SỬA)
    // =======================================================
    @FXML
    private void handleCustomerLogin(ActionEvent event) {

        String user = custUser.getText();
        String pass = custPass.getText();

        if (user.isBlank() || pass.isBlank()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        int currentPcId = 1; // hoặc set từ MainApp

        Optional<Computer> pcOpt =
                AppContext.computerRepo.findAll()
                        .stream()
                        .filter(c -> c.getComputerId() == currentPcId)
                        .findFirst();

        if (pcOpt.isEmpty()) {
            showAlert("Lỗi hệ thống",
                    "Không tìm thấy máy trạm PC-" + currentPcId);
            return;
        }

        try {
            Session session =
                    clientAuth.loginCustomer(
                            user, pass, pcOpt.get());

            if (session == null) {
                showAlert("Đăng nhập thất bại",
                        "Sai tài khoản/mật khẩu hoặc tài khoản bị khóa!");
                return;
            }

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(
                            "/fxml/client/client_dashboard.fxml"));

            Parent root = loader.load();

            ClientController clientCtrl =
                    loader.getController();
            clientCtrl.setSession(session);

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("CyberGame - Client");
            stage.show();

        } catch (IllegalStateException e) {
            // 🔥 tài khoản đang online
            showAlert("Không thể đăng nhập", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    // =======================================================
    // UTILS
    // =======================================================
    private void navigate(ActionEvent event,
                          String fxmlPath,
                          String title) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(fxmlPath));

            Parent root = loader.load();

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện",
                    "Không tìm thấy file: " + fxmlPath);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert =
                new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
