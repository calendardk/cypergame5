package com.cybergame.ui.fxcontroller;

import com.cybergame.controller.ServiceItemController;
import com.cybergame.model.entity.ServiceItem;
import com.cybergame.model.enums.ServiceCategory;
import com.cybergame.repository.sql.ServiceItemRepositorySQL;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProductMgmtController implements Initializable {

    // --- FXML ELEMENTS ---
    @FXML private TableView<ServiceItem> productTable;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private ComboBox<String> cmbCategoryFilter;
    @FXML private Label lblTotal;

    @FXML private TableColumn<ServiceItem, Integer> colId;
    @FXML private TableColumn<ServiceItem, String> colName;
    @FXML private TableColumn<ServiceItem, String> colCategory;
    @FXML private TableColumn<ServiceItem, Double> colPrice;
    @FXML private TableColumn<ServiceItem, String> colStatus;

    // --- DEPENDENCIES ---
    private final ServiceItemRepositorySQL serviceRepo = new ServiceItemRepositorySQL();
    private final ServiceItemController serviceCtrl = new ServiceItemController(serviceRepo);

    private ObservableList<ServiceItem> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
        setupSearch();

        // Cấu hình bộ lọc
        cmbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Đang bán", "Ngừng bán"));
        cmbStatusFilter.setValue("Tất cả");

        List<String> categories = new ArrayList<>();
        categories.add("Tất cả");
        categories.addAll(Arrays.stream(ServiceCategory.values())
                                .map(Enum::name)
                                .collect(Collectors.toList()));
        cmbCategoryFilter.setItems(FXCollections.observableArrayList(categories));
        cmbCategoryFilter.setValue("Tất cả");

        cmbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> triggerSearch());
        cmbCategoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> triggerSearch());

        setupRowSelection();
        Platform.runLater(() -> productTable.getSelectionModel().clearSelection());
    }

    private void triggerSearch() {
        String currentSearch = txtSearch.getText();
        txtSearch.setText(currentSearch + " ");
        txtSearch.setText(currentSearch);
    }

    private void setupRowSelection() {
        productTable.setRowFactory(tv -> {
            TableRow<ServiceItem> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (!row.isEmpty() && row.isSelected() && e.getButton() == MouseButton.PRIMARY) {
                    productTable.getSelectionModel().clearSelection();
                    e.consume();
                }
            });
            return row;
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("serviceId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory().toString()));
        colCategory.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #fcd34d;");
                }
            }
        });

        colPrice.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getUnitPrice()).asObject());
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%,.0f đ", item));
                if (!empty) {
                    setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        colStatus.setCellValueFactory(cell -> {
            boolean locked = cell.getValue().isLocked();
            return new SimpleStringProperty(locked ? "Ngừng bán" : "Đang bán");
        });

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if ("Ngừng bán".equals(item)) {
                        setTextFill(Color.RED);
                        setStyle("-fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setTextFill(Color.LIGHTGREEN);
                        setStyle("-fx-font-weight: normal; -fx-alignment: CENTER;");
                    }
                }
            }
        });
    }

    private void loadData() {
        if (serviceRepo != null) {
            masterData.setAll(serviceRepo.findAll());
            updateTotalLabel();
        }
    }

    private void updateTotalLabel() {
        if (lblTotal != null) {
            lblTotal.setText(String.valueOf(masterData.size()));
        }
    }

    private void setupSearch() {
        FilteredList<ServiceItem> filteredData = new FilteredList<>(masterData, p -> true);

        txtSearch.textProperty().addListener((obs, oldVal, newValue) -> {
            filteredData.setPredicate(item -> {
                String statusFilter = cmbStatusFilter.getValue();
                boolean matchStatus = true;
                if ("Đang bán".equals(statusFilter)) matchStatus = !item.isLocked();
                else if ("Ngừng bán".equals(statusFilter)) matchStatus = item.isLocked();

                String catFilter = cmbCategoryFilter.getValue();
                boolean matchCategory = true;
                if (catFilter != null && !"Tất cả".equals(catFilter)) {
                    matchCategory = item.getCategory().name().equals(catFilter);
                }

                boolean matchSearch = true;
                if (newValue != null && !newValue.isEmpty()) {
                    matchSearch = item.getName().toLowerCase().contains(newValue.toLowerCase());
                }

                return matchStatus && matchCategory && matchSearch;
            });
        });

        SortedList<ServiceItem> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sortedData);
    }

    @FXML
    private void onAdd() {
        showDialog(null);
    }

    @FXML
    private void onUpdate() {
        ServiceItem selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn món cần sửa!");
            return;
        }
        showDialog(selected);
    }

    @FXML
    private void onLockUnlock() {
        ServiceItem selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn món để thao tác!");
            return;
        }

        if (selected.isLocked()) {
            selected.unlock();
            showAlert("Thành công", "Đã mở bán lại món: " + selected.getName());
        } else {
            selected.lock();
            showAlert("Thành công", "Đã tạm ngừng bán món: " + selected.getName());
        }
        serviceRepo.save(selected);
        productTable.refresh();
    }

    @FXML
    private void onDelete() {
        ServiceItem selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn món cần xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc muốn xóa: " + selected.getName() + "?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                serviceCtrl.delete(selected);
                masterData.remove(selected);
                productTable.getSelectionModel().clearSelection();
                updateTotalLabel();
            } catch (Exception e) {
                showAlert("Lỗi", "Không thể xóa (Có thể món này đã có trong đơn hàng cũ).");
            }
        }
    }

    private void showDialog(ServiceItem existingItem) {
        Dialog<ServiceItem> dialog = new Dialog<>();
        dialog.setTitle(existingItem == null ? "Thêm Món Mới" : "Sửa Thông Tin Món");
        dialog.setHeaderText(null);

        ButtonType btnSave = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField txtName = new TextField();
        txtName.setPromptText("Tên dịch vụ/món ăn");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Đơn giá (VNĐ)");

        ComboBox<ServiceCategory> cmbCategory = new ComboBox<>();
        cmbCategory.setItems(FXCollections.observableArrayList(ServiceCategory.values()));
        cmbCategory.setPromptText("Chọn loại");

        if (existingItem != null) {
            txtName.setText(existingItem.getName());
            txtPrice.setText(String.format("%.0f", existingItem.getUnitPrice()));
            cmbCategory.setValue(existingItem.getCategory());
        } else {
            cmbCategory.getSelectionModel().selectFirst();
        }

        grid.add(new Label("Tên món:"), 0, 0); grid.add(txtName, 1, 0);
        grid.add(new Label("Loại:"), 0, 1);    grid.add(cmbCategory, 1, 1);
        grid.add(new Label("Giá bán:"), 0, 2); grid.add(txtPrice, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(txtName::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == btnSave) {
                try {
                    String name = txtName.getText().trim();
                    double price = Double.parseDouble(txtPrice.getText().trim());
                    ServiceCategory cat = cmbCategory.getValue();

                    if (name.isEmpty() || cat == null) return null;

                    // --- SỬA LỖI Ở ĐÂY ---
                    // Dùng đúng constructor có 4 tham số: id, name, price, category
                    return new ServiceItem(
                        existingItem == null ? 0 : existingItem.getServiceId(),
                        name,
                        price,
                        cat 
                    );
                    // ---------------------
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<ServiceItem> result = dialog.showAndWait();
        result.ifPresent(formData -> {
            if (formData.getName().isEmpty()) {
                showAlert("Lỗi", "Tên món không hợp lệ!");
                return;
            }

            try {
                if (existingItem == null) {
                    // Tạo mới
                    ServiceItem newItem = serviceCtrl.createService(
                        formData.getName(),
                        formData.getUnitPrice(),
                        formData.getCategory()
                    );
                    masterData.add(newItem);
                    showAlert("Thành công", "Đã thêm mới: " + newItem.getName());
                } else {
                    // Update
                    existingItem.setName(formData.getName());
                    existingItem.setUnitPrice(formData.getUnitPrice());
                    existingItem.setCategory(formData.getCategory());
                    
                    serviceRepo.save(existingItem);
                    productTable.refresh();
                }
                updateTotalLabel();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Có lỗi xảy ra: " + e.getMessage());
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}