package com.sanxynet.bluetoothprinter;

import com.google.gson.JsonObject;

public class Orders {

    //private long id;
    private String id;
    private String name;
    private String address;
    private String mobile;
    private String orders;
    private String status;
    private String created_at;

    public Orders(JsonObject obj)
    {
        id = obj.get("id").getAsString();
        name = obj.get("name").getAsString();
        address = obj.get("address").getAsString();
        mobile = obj.get("mobile").getAsString();
        orders = obj.get("orders").getAsString();
        status = obj.get("status").getAsString();
        created_at = obj.get("created_at").getAsString();
    }

    public String getOrderId() {
        return id;
    }

    public String getOrderName() {
        return name;
    }

    public String getOrderAddress() {
        return address;
    }

    public String getOrderMobile() {
        return mobile;
    }

    public String getOrders() {
        return orders;
    }

    public String getOrderStatus() {
        return status;
    }

    public String getOrderCreatedAt() {
        return created_at;
    }

}
