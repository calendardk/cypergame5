package com.cybergame.model.entity;

import com.cybergame.model.enums.OrderStatus;
import com.cybergame.model.enums.PaymentSource;
import com.cybergame.model.enums.OrderStatus;
import java.time.LocalDateTime;
import com.cybergame.util.TimeUtil;

import java.io.Serializable;

public class OrderItem implements Serializable {

    private int orderItemId;
    private ServiceItem serviceItem;
    private int quantity;
    private double unitPriceAtOrder;
    private PaymentSource paymentSource;
    private OrderStatus status;
    private LocalDateTime orderedAt;


    public OrderItem(int id, ServiceItem service,
                     int quantity, PaymentSource source) {
        this.orderItemId = id;
        this.serviceItem = service;
        this.quantity = quantity;
        this.unitPriceAtOrder = service.getUnitPrice();
        this.paymentSource = source;
        this.status = OrderStatus.PENDING;
        this.orderedAt = TimeUtil.nowVN();
    }

    public double getCost() {
        return unitPriceAtOrder * quantity;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public PaymentSource getPaymentSource() {
        return paymentSource;
    }
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public int getQuantity() {
        return quantity;
    }
    

}