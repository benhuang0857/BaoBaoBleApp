package com.benhuang.baobaobluetoothprinter;

import com.google.gson.JsonObject;

public class Orders {

    private String id; // 獨立訂單號(提供系統查詢用)
    private String ordernum; // 單號(提供廚房叫號用)
    private String name;
    private String address;
    private String mobile;
    private String orders;
    private String discount;
    private String total;
    private String status;
    private String tableware; // 餐具
    private String created_at;

    public Orders(JsonObject obj)
    {
        id = obj.get("id").getAsString(); // 獨立訂單號(提供系統查詢用)
        ordernum = obj.get("ordernum").getAsString(); // 單號(提供廚房叫號用)
        name = obj.get("name").getAsString(); // 姓名
        address = obj.get("address").getAsString(); // ex 台中市太平區文華街100號
        mobile = obj.get("mobile").getAsString();
        orders = obj.get("orders").getAsString(); // ex "雞腿X1 | 薯條X2"
        discount = obj.get("discount").getAsString();
        total = obj.get("total").getAsString();
        total = obj.get("tableware").getAsString();
        status = obj.get("status").getAsString(); // printed or unprint
        created_at = obj.get("created_at").getAsString();
    }

    public String getOrderId() {
        return id;
    }

    public String getOrderNum() { return ordernum; }

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

    public String getTableware() { return tableware; }

    public String getOrderStatus() {
        return status;
    }

    public String getOrderCreatedAt() {
        return created_at;
    }

}
