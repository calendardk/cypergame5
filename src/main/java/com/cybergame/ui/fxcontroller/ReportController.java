package com.cybergame.ui.fxcontroller;

import com.cybergame.model.entity.Invoice;
import com.cybergame.model.entity.OrderItem;
import com.cybergame.model.entity.TopUpHistory;
import com.cybergame.model.enums.PaymentSource;
import com.cybergame.repository.sql.InvoiceRepositorySQL;
import com.cybergame.repository.sql.TopUpHistoryRepositorySQL;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportController implements Initializable {

    private final InvoiceRepositorySQL invoiceRepo = new InvoiceRepositorySQL();
    private final TopUpHistoryRepositorySQL topUpRepo = new TopUpHistoryRepositorySQL();
    private final Map<OrderItem, String> orderOwnerMap = new HashMap<>();

    // --- FXML: STATS ---
    @FXML private Label lblTotalRealRevenue;
    @FXML private Label lblTotalTopUp;
    @FXML private Label lblMachineRevenue;
    @FXML private Label lblServiceRevenue;
    @FXML private Label lblServiceDetail;

    // --- FXML: FILTER ---
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    // --- TABLE: HÓA ĐƠN ---
    @FXML private TableView<Invoice> tableInvoices;
    @FXML private TableColumn<Invoice, Integer> colInvId;
    @FXML private TableColumn<Invoice, String> colInvTime;
    @FXML private TableColumn<Invoice, String> colInvCustomer;
    @FXML private TableColumn<Invoice, String> colInvComputer;
    @FXML private TableColumn<Invoice, Double> colInvMachine;
    @FXML private TableColumn<Invoice, Double> colInvSvcCash;
    @FXML private TableColumn<Invoice, Double> colInvSvcAcc;
    @FXML private TableColumn<Invoice, Double> colInvTotal;
    @FXML private TableColumn<Invoice, Void> colInvAction;

    // --- TABLE: CHI TIẾT ORDER ---
    @FXML private TableView<OrderItem> tableOrderHistory;
    @FXML private TableColumn<OrderItem, String> colOrdTime;
    @FXML private TableColumn<OrderItem, String> colOrdCustomer;
    @FXML private TableColumn<OrderItem, String> colOrdName;
    @FXML private TableColumn<OrderItem, Integer> colOrdQty;
    @FXML private TableColumn<OrderItem, Double> colOrdPrice;
    @FXML private TableColumn<OrderItem, Double> colOrdTotal;
    @FXML private TableColumn<OrderItem, String> colOrdSource;
    @FXML private TableColumn<OrderItem, String> colOrdStatus;

    // --- TABLE: NẠP TIỀN ---
    @FXML private TableView<TopUpHistory> tableTopUps;
    @FXML private TableColumn<TopUpHistory, Integer> colTopId;
    @FXML private TableColumn<TopUpHistory, String> colTopTime;
    @FXML private TableColumn<TopUpHistory, String> colTopCustomer;
    @FXML private TableColumn<TopUpHistory, String> colTopRole;
    @FXML private TableColumn<TopUpHistory, String> colTopStaffId;
    @FXML private TableColumn<TopUpHistory, String> colTopOperator;
    @FXML private TableColumn<TopUpHistory, Double> colTopAmount;
    @FXML private TableColumn<TopUpHistory, String> colTopNote;
    @FXML private TableColumn<TopUpHistory, Void> colTopAction;

    // --- TABLE: THỐNG KÊ ---
    @FXML private TableView<ProductStat> tableProductStats;
    @FXML private TableColumn<ProductStat, String> colStatName;
    @FXML private TableColumn<ProductStat, Integer> colStatQty;
    @FXML private TableColumn<ProductStat, Double> colStatRevenue;

    private final ObservableList<Invoice> invoiceList = FXCollections.observableArrayList();
    private final ObservableList<TopUpHistory> topUpList = FXCollections.observableArrayList();
    private final ObservableList<OrderItem> orderList = FXCollections.observableArrayList();
    private final ObservableList<ProductStat> productStatList = FXCollections.observableArrayList();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupInvoiceTable();
        setupOrderTable();
        setupTopUpTable();
        setupProductStatTable();

        // 1. Áp dụng Logic Màu & Click cho TẤT CẢ các bảng
        setupTableStyle(tableInvoices);
        setupTableStyle(tableOrderHistory);
        setupTableStyle(tableTopUps);
        setupTableStyle(tableProductStats);

        // Cho phép chiều cao hàng tự động điều chỉnh
        tableInvoices.setFixedCellSize(-1);
        tableOrderHistory.setFixedCellSize(-1);
        tableTopUps.setFixedCellSize(-1);
        tableProductStats.setFixedCellSize(-1);

        dpFrom.setValue(LocalDate.now());
        dpTo.setValue(LocalDate.now());
        loadData();
    }

    // ================== HÀM DÙNG CHUNG ĐỂ SETUP STYLE & CLICK ==================
    private <T> void setupTableStyle(TableView<T> table) {
        // A. Cấu hình màu nền tối cho bảng
        table.setStyle("-fx-control-inner-background: #0d1b2a; -fx-base: #0d1b2a; -fx-background-color: #0d1b2a;");

        // B. Cấu hình Row Factory (Toggle Click + Màu Xanh Dương)
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();

            // 1. Logic Toggle: Ấn lại vào dòng đang chọn thì bỏ chọn
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (!row.isEmpty() && row.isSelected() && e.getButton() == MouseButton.PRIMARY) {
                    table.getSelectionModel().clearSelection();
                    e.consume();
                }
            });

            // 2. Logic Tô Màu: Khi chọn -> Xanh Dương (#007bff)
            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (!row.isEmpty()) {
                    if (isSelected) {
                        row.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        row.setStyle(""); // Reset về style mặc định (trong suốt)
                    }
                }
            });

            // 3. Logic Tô Màu: Khi dữ liệu thay đổi (Scroll/Refresh)
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("-fx-background-color: transparent;"); // Dòng trống
                } else if (row.isSelected()) {
                    row.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
                } else {
                    row.setStyle("");
                }
            });

            return row;
        });

        // C. Clear selection lúc đầu
        Platform.runLater(() -> table.getSelectionModel().clearSelection());
    }

    // ================== SETUP TABLES ==================
    private void setupInvoiceTable() {
        colInvId.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        
        colInvTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt().format(dtf)));
        enableTextWrap(colInvTime);
        
        colInvCustomer.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        enableTextWrap(colInvCustomer);
        
        colInvComputer.setCellValueFactory(new PropertyValueFactory<>("computerName"));
        enableTextWrap(colInvComputer);

        colInvSvcCash.setCellValueFactory(cell -> calculateServiceAmount(cell.getValue(), PaymentSource.CASH));
        formatCurrencyColumn(colInvSvcCash, "#fbbf24"); // Vàng

        colInvSvcAcc.setCellValueFactory(cell -> calculateServiceAmount(cell.getValue(), PaymentSource.ACCOUNT));
        formatCurrencyColumn(colInvSvcAcc, "white"); // Trắng

        colInvMachine.setCellValueFactory(cell -> {
            Invoice inv = cell.getValue();
            double totalSvc = (inv.getOrderItems() != null) ?
                    inv.getOrderItems().stream().mapToDouble(OrderItem::getCost).sum() : 0;
            return new SimpleDoubleProperty(Math.max(0, inv.getTotalAmount() - totalSvc)).asObject();
        });
        formatCurrencyColumn(colInvMachine, "#34d399"); // Xanh lá

        colInvTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        formatCurrencyColumn(colInvTotal, "#f472b6"); // Hồng

        // Nút Xem / Xóa
        colInvAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(5, btnView, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);
                btnView.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                
                btnView.setOnAction(e -> showInvoiceDetail(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteInvoice(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        tableInvoices.setItems(invoiceList);
    }

    private void setupOrderTable() {
        colOrdTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrderedAt().format(dtf)));
        enableTextWrap(colOrdTime);
        
        colOrdCustomer.setCellValueFactory(cell -> new SimpleStringProperty(orderOwnerMap.getOrDefault(cell.getValue(), "Vãng lai")));
        colOrdCustomer.setCellFactory(tc -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(tc.widthProperty().subtract(10));
                text.setFill(Color.web("#a78bfa")); // Tím nhạt
                text.setStyle("-fx-font-weight: bold;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                }
            }
        });
        
        colOrdName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getServiceItem().getName()));
        enableTextWrap(colOrdName);

        colOrdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        colOrdPrice.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getServiceItem().getUnitPrice()));
        formatCurrencyColumn(colOrdPrice, "white");
        
        colOrdTotal.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getCost()));
        formatCurrencyColumn(colOrdTotal, "#fbbf24"); // Vàng
        
        colOrdSource.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPaymentSource().toString()));
        enableTextWrap(colOrdSource);
        
        colOrdStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().toString()));
        enableTextWrap(colOrdStatus);
        
        tableOrderHistory.setItems(orderList);
    }

    private void setupTopUpTable() {
        colTopId.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        colTopTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt().format(dtf)));
        enableTextWrap(colTopTime);
        
        colTopCustomer.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        enableTextWrap(colTopCustomer);
        
        colTopRole.setCellValueFactory(new PropertyValueFactory<>("operatorType"));
        enableTextWrap(colTopRole);
        
        colTopStaffId.setCellValueFactory(cell -> {
            Integer id = cell.getValue().getOperatorId();
            return new SimpleStringProperty((id == null || id == 0) ? "-" : String.valueOf(id));
        });
        
        colTopOperator.setCellValueFactory(new PropertyValueFactory<>("operatorName"));
        enableTextWrap(colTopOperator);
        
        colTopAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        formatCurrencyColumn(colTopAmount, "#f472b6"); // Hồng
        
        colTopNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        enableTextWrap(colTopNote);

        colTopAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑 Xóa");
            {
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> deleteTopUp(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(empty ? null : btnDelete);
            }
        });

        tableTopUps.setItems(topUpList);
    }

    private void setupProductStatTable() {
        colStatName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        enableTextWrap(colStatName);
        
        colStatQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colStatQty.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: white;");
        
        colStatRevenue.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        formatCurrencyColumn(colStatRevenue, "#fbbf24");
        
        tableProductStats.setItems(productStatList);
    }

    // ================== LOGIC ==================
    @FXML private void onRefresh() {
        dpFrom.setValue(null); 
        dpTo.setValue(null);
        loadData();
    }
    
    @FXML private void onFilter() { 
        loadData(); 
    }

    private void loadData() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        orderOwnerMap.clear();

        // 1. TopUp
        List<TopUpHistory> filteredTopUps = topUpRepo.findAll().stream()
            .filter(t -> isWithinDate(t.getCreatedAt().toLocalDate(), from, to))
            .collect(Collectors.toList());
        topUpList.setAll(filteredTopUps);

        // 2. Invoice
        List<Invoice> filteredInvoices = invoiceRepo.findAll().stream()
            .filter(i -> isWithinDate(i.getCreatedAt().toLocalDate(), from, to))
            .collect(Collectors.toList());
        invoiceList.setAll(filteredInvoices);

        // 3. Stats
        double totalMachine = 0, totalSvcCash = 0, totalSvcAcc = 0;
        List<OrderItem> allOrderItems = new ArrayList<>();
        Map<String, ProductStat> statMap = new HashMap<>();

        for (Invoice inv : filteredInvoices) {
            double invSvcCash = 0, invSvcAcc = 0;
            if (inv.getOrderItems() != null) {
                for (OrderItem item : inv.getOrderItems()) {
                    allOrderItems.add(item);
                    orderOwnerMap.put(item, inv.getAccountName());

                    if (item.getPaymentSource() == PaymentSource.CASH) invSvcCash += item.getCost();
                    else invSvcAcc += item.getCost();

                    statMap.computeIfAbsent(item.getServiceItem().getName(), ProductStat::new)
                           .add(item.getQuantity(), item.getCost());
                }
            }
            totalMachine += Math.max(0, inv.getTotalAmount() - (invSvcCash + invSvcAcc));
            totalSvcCash += invSvcCash;
            totalSvcAcc += invSvcAcc;
        }

        orderList.setAll(allOrderItems);
        productStatList.setAll(statMap.values());
        productStatList.sort((p1, p2) -> Integer.compare(p2.getQuantity(), p1.getQuantity()));

        // Update Labels
        lblTotalTopUp.setText(String.format("%,.0f VNĐ", filteredTopUps.stream().mapToDouble(TopUpHistory::getAmount).sum()));
        lblTotalRealRevenue.setText(String.format("%,.0f VNĐ", filteredTopUps.stream().mapToDouble(TopUpHistory::getAmount).sum() + totalSvcCash));
        lblMachineRevenue.setText(String.format("%,.0f VNĐ", totalMachine));
        lblServiceRevenue.setText(String.format("%,.0f VNĐ", totalSvcCash + totalSvcAcc));
        lblServiceDetail.setText(String.format("(TM: %,.0f - TK: %,.0f)", totalSvcCash, totalSvcAcc));
    }

    // ================== CHI TIẾT & IN HÓA ĐƠN ==================
    private void showInvoiceDetail(Invoice inv) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Hóa Đơn #" + inv.getInvoiceId());
        
        VBox receiptBox = new VBox(5);
        receiptBox.setPadding(new Insets(20));
        receiptBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1px;");
        receiptBox.setPrefWidth(350);

        // Header
        Label lblHeader = new Label("CYBER GAME CENTER");
        lblHeader.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lblHeader.setTextFill(Color.BLACK);
        
        Label lblSubHeader = new Label("--------------------------------");
        lblSubHeader.setTextFill(Color.GRAY);

        // Info
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10); infoGrid.setVgap(5);
        infoGrid.addRow(0, new Label("Hóa đơn:"), new Label("#" + inv.getInvoiceId()));
        infoGrid.addRow(1, new Label("Ngày:"), new Label(inv.getCreatedAt().format(dtf)));
        infoGrid.addRow(2, new Label("Khách hàng:"), new Label(inv.getAccountName()));
        infoGrid.addRow(3, new Label("Máy:"), new Label(inv.getComputerName()));
        
        // Items Table
        VBox itemsBox = new VBox(5);
        itemsBox.setPadding(new Insets(10, 0, 10, 0));
        itemsBox.getChildren().add(new Label("--------------------------------"));
        
        HBox headerRow = new HBox();
        headerRow.getChildren().addAll(
            createLabel("Món/DV", 140, true),
            createLabel("SL", 30, true),
            createLabel("T.Tiền", 80, false)
        );
        itemsBox.getChildren().add(headerRow);
        
        double serviceTotal = 0;
        if (inv.getOrderItems() != null) {
            for (OrderItem item : inv.getOrderItems()) {
                HBox row = new HBox();
                row.getChildren().addAll(
                    createLabel(item.getServiceItem().getName(), 140, false),
                    createLabel("x" + item.getQuantity(), 30, false),
                    createLabel(String.format("%,.0f", item.getCost()), 80, false)
                );
                itemsBox.getChildren().add(row);
                serviceTotal += item.getCost();
            }
        }
        
        itemsBox.getChildren().add(new Label("--------------------------------"));

        // Totals
        double machineFee = Math.max(0, inv.getTotalAmount() - serviceTotal);
        GridPane totalGrid = new GridPane();
        totalGrid.setHgap(10); totalGrid.setVgap(5);
        totalGrid.setAlignment(Pos.CENTER_RIGHT);
        
        totalGrid.addRow(0, new Label("Tiền giờ:"), new Label(String.format("%,.0f", machineFee)));
        totalGrid.addRow(1, new Label("Dịch vụ:"), new Label(String.format("%,.0f", serviceTotal)));
        
        Label lblTotal = new Label(String.format("%,.0f VNĐ", inv.getTotalAmount()));
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        totalGrid.addRow(2, new Label("TỔNG CỘNG:"), lblTotal);

        Label lblFooter = new Label("Cảm ơn quý khách!");
        lblFooter.setStyle("-fx-font-style: italic;");
        
        receiptBox.getChildren().addAll(lblHeader, lblSubHeader, infoGrid, itemsBox, totalGrid, new Label(""), lblFooter);
        receiptBox.setAlignment(Pos.TOP_CENTER);

        // Buttons
        Button btnPrint = new Button("🖨 In / Xuất Hóa Đơn");
        btnPrint.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnPrint.setOnAction(e -> printInvoice(receiptBox));

        Button btnClose = new Button("Đóng");
        btnClose.setOnAction(e -> dialog.setResult(null));

        HBox buttonBox = new HBox(10, btnPrint, btnClose);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));

        VBox root = new VBox(receiptBox, buttonBox);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        
        dialog.show();
    }

    private Label createLabel(String text, double width, boolean bold) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-text-fill: black;");
        if (bold) l.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        if (width == 80) l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    private void printInvoice(Node node) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            boolean success = job.printPage(node);
            if (success) {
                job.endJob();
                new Alert(Alert.AlertType.INFORMATION, "Đã gửi lệnh in thành công!").show();
            } else {
                new Alert(Alert.AlertType.ERROR, "Lỗi khi in hóa đơn.").show();
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Không tìm thấy máy in kết nối.").show();
        }
    }

    // ================== HELPERS ==================
    private void deleteInvoice(Invoice inv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa Hóa đơn #" + inv.getInvoiceId() + "?\nDữ liệu sẽ thay đổi.", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            invoiceList.remove(inv);
            loadData();
        }
    }

    private void deleteTopUp(TopUpHistory topUp) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Xóa lịch sử nạp #" + topUp.getId() + "?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            topUpList.remove(topUp);
            loadData();
        }
    }

    // Tự động xuống dòng (Text Wrap)
    private <T> void enableTextWrap(TableColumn<T, String> col) {
        col.setCellFactory(tc -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                text.setFill(Color.WHITE);
                text.setTextAlignment(TextAlignment.LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                }
            }
        });
    }

    private SimpleObjectProperty<Double> calculateServiceAmount(Invoice inv, PaymentSource source) {
        double amount = (inv.getOrderItems() != null) ? 
            inv.getOrderItems().stream()
               .filter(o -> o.getPaymentSource() == source)
               .mapToDouble(OrderItem::getCost).sum() : 0;
        return new SimpleObjectProperty<>(amount);
    }

    private boolean isWithinDate(LocalDate date, LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    // Format tiền tệ & Màu sắc (Giữ màu kể cả khi selected)
    private <T> void formatCurrencyColumn(TableColumn<T, Double> col, String colorHex) {
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(String.format("%,.0f", item));
                    // Luôn giữ màu chữ chỉ định (Vàng/Xanh/Hồng) để nổi trên nền Xanh Dương hoặc Tối
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
                }
            }
        });
    }

    public static class ProductStat {
        private String productName;
        private int quantity;
        private double totalRevenue;
        
        public ProductStat(String productName) { 
            this.productName = productName; 
        }
        
        public void add(int q, double r) { 
            this.quantity += q; 
            this.totalRevenue += r; 
        }
        
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getTotalRevenue() { return totalRevenue; }
    }
}