package com.cybergame.ui.fxcontroller;

import com.cybergame.model.entity.Employee;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class StaffDashboardController implements Initializable {

    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Label lblStaffName; // Label hiển thị tên nhân viên trên Topbar

    private boolean sidebarVisible = true;
    private Employee currentEmployee; // Lưu thông tin nhân viên đang đăng nhập

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mặc định load màn hình Sơ đồ máy trạm khi vào Dashboard
        showMachineMap();
    }

    // --- [QUAN TRỌNG] HÀM NHẬN DỮ LIỆU TỪ LOGIN CONTROLLER ---
    public void setStaffInfo(Employee emp) {
        this.currentEmployee = emp;
        if (emp != null) {
            // Ưu tiên hiển thị Tên đầy đủ (DisplayName), nếu không có thì dùng Username
            String name = (emp.getDisplayName() != null && !emp.getDisplayName().isEmpty()) 
                          ? emp.getDisplayName() 
                          : emp.getUsername();
            
            lblStaffName.setText(name); 
            System.out.println("Dashboard Staff: Đã nhận thông tin nhân viên " + name);
        }
    }

    // =======================================================
    // CÁC CHỨC NĂNG MENU (ĐIỀU HƯỚNG)
    // =======================================================

    @FXML
    private void showOverview() {
        // Dùng chung giao diện Tổng quan của Admin
        loadView("/fxml/admin/admin_overview.fxml");
    }

    @FXML
    private void showMachineMap() {
        // Dùng chung giao diện Quản lý máy của Admin
        loadView("/fxml/admin/machine_mgmt.fxml");
    }

    @FXML
    private void showSessionMgmt() {
        // Dùng chung giao diện Quản lý phiên chơi của Admin
        loadView("/fxml/admin/session_mgmt.fxml");
    }
    
    @FXML
    private void showOrderRequests() {
        // Giao diện xử lý Order dành riêng cho Staff
        loadView("/fxml/staff/order_request.fxml");
    }

    // --- [CẬP NHẬT] GỌI FILE QUẢN LÝ KHÁCH MỚI CỦA STAFF ---
    @FXML
    private void showCustomerMgmt() {
        try {
            // 1. Load file FXML mới dành riêng cho nhân viên
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/staff/customer_mgmt.fxml"));
            Parent view = loader.load();
            
            // 2. Lấy Controller của nó và truyền thông tin nhân viên vào (để nạp tiền đúng tên)
            StaffCustomerMgmtController ctrl = loader.getController();
            ctrl.setStaffInfo(this.currentEmployee); 
            
            // 3. Hiển thị lên màn hình chính
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Lỗi tải màn hình Quản lý khách (Staff): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =======================================================
    // CHỨC NĂNG HỆ THỐNG (MENU, LOGOUT)
    // =======================================================

    @FXML
    private void handleToggleMenu() {
        sidebarVisible = !sidebarVisible;
        sidebar.setManaged(sidebarVisible);
        sidebar.setVisible(sidebarVisible);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Quay lại màn hình đăng nhập Staff
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login/staff_login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("CyberGame - Login");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- HÀM HỖ TRỢ LOAD VIEW ĐƠN GIẢN ---
    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Lỗi không tìm thấy file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
}