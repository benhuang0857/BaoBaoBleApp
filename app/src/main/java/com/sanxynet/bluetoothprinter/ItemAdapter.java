package com.sanxynet.bluetoothprinter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ItemAdapter  extends RecyclerView.Adapter<ItemAdapter.ViewHolder>{
    private ArrayList<Orders> data;

    //建構子，順便傳入要顯示的資料
    public ItemAdapter(ArrayList<Orders> data){
        this.data = data;
    }

    /*
     * 繼承RecyclerView.Adapter 必須實作2個functions & 1 method
     * 分別為：  onCreateViewHolder、onBindViewHolder、getItemCount
     * */

    //onCreateViewHolder 在 RecyclerView 需要新的 ViewHolder時被呼叫
    //比較需要注意LayoutInflater.infalte() 的第三個參數(boolean attachToRoot)必須設為false
    //不然會拋出java.lang.IllegalStateException
    @NonNull
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycle_item, viewGroup, false);
        return new ViewHolder(v);
    }

    //onBindViewHolder 在RecyclerView 在特定的位置要顯示資料時被呼叫
    //第一個參數為ViewHolder，第二個參數是位置
    //通常會在這設定layout裡對應的元件要顯示的內容
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Orders info = data.get(i);
        viewHolder.Orders.setText(info.getOrders());
    }

    //getItemCount 回傳list裡面item的總數
    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private TextView OrderId, OrderName, OrderAddress, OrderMobile, Orders, OrderStatus, OrderCreatedAt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            OrderId = itemView.findViewById(R.id.printOrderId);
            OrderName = itemView.findViewById(R.id.printOrderName);
            OrderAddress = itemView.findViewById(R.id.printOrderAddress);
            OrderMobile = itemView.findViewById(R.id.printOrderMobile);
            Orders = itemView.findViewById(R.id.printOrders);
        }

    }
}
