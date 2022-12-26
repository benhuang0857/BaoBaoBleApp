package com.benhuang.baobaobluetoothprinter;

import com.google.gson.JsonObject;

public class Orders {

    //private long id;
    private String id;
    private String name;
    private String address;
    private String mobile;
    private String orders;
    private String discount;
    private String total;
    private String status;
    private String created_at;

    public Orders(JsonObject obj)
    {
        id = obj.get("id").getAsString(); // 訂單號
        name = obj.get("name").getAsString(); // 姓名
        address = obj.get("address").getAsString(); // ex 台中市太平區文華街100號
        mobile = obj.get("mobile").getAsString();
        orders = obj.get("orders").getAsString(); // ex "雞腿X1 79元 | 薯條X2 106元"
        discount = obj.get("discount").getAsString();
        total = obj.get("total").getAsString();
        status = obj.get("status").getAsString(); // printed or unprint
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

    public String getDiscount() {
        return discount;
    }

    public String getTotal() {
        return total;
    }

    public String getOrderStatus() {
        return status;
    }

    public String getOrderCreatedAt() {
        return created_at;
    }

}
