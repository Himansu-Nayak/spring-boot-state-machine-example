package com.example.orderservice.entity;

import com.example.orderservice.enums.OrderStates;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ORDERS")
public class Order {

    @Id
    @GeneratedValue
    private Long id;
    private Date datetime;

    private String state;

    public Order(Date date, OrderStates states) {
        this.datetime = date;
        this.state = states.name();
    }

    public OrderStates getOrderState() {
        return OrderStates.valueOf(this.state);
    }

    public void setOrderState(OrderStates state) {
        this.state = state.name();
    }

}