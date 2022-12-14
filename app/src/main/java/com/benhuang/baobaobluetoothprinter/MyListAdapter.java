package com.benhuang.baobaobluetoothprinter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder>{

    private Context mContext;
    private List<HashMap<String, String>> mDatas;

    public MyListAdapter(Context context, List<HashMap<String, String>> datas) {
        mContext = context;
        mDatas = datas;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView txtItem,printOrderId,printOrderName,printOrderAddress,printOrders,printOrderMobile;
        private Button btnPrint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            printOrderId = itemView.findViewById(R.id.printOrderId);
            printOrderName = itemView.findViewById(R.id.printOrderName);
            printOrderAddress = itemView.findViewById(R.id.printOrderAddress);
            printOrderMobile = itemView.findViewById(R.id.printOrderMobile);
            printOrders  = itemView.findViewById(R.id.printOrders);
            txtItem = (TextView) itemView.findViewById(R.id.printOrderMobile);
            btnPrint = (Button) itemView.findViewById(R.id.button3);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycle_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.printOrderId.setText(mDatas.get(position).get("Id"));
        holder.printOrderName.setText(mDatas.get(position).get("Customer"));
        holder.printOrderAddress.setText(mDatas.get(position).get("Address"));
        holder.printOrderMobile.setText(mDatas.get(position).get("Mobile"));
        holder.printOrders.setText(mDatas.get(position).get("Orders"));

//        holder.btnPrint.setOnClickListener((v)->{
//            // freshHandler.sendEmptyMessage(1);
//            // printContext(mDatas.get(position).get("Customer"));
//            onBtnPrintClickListener.onBtnPrintClick(mDatas.get(position).get("Customer"));
//        });

        holder.btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick:", mDatas.get(position).get("Customer"));
//                onBtnPrintClickListener.onBtnPrintClick(mDatas.get(position).get("Customer"));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }
}

