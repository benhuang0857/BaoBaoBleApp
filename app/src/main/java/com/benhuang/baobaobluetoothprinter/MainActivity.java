package com.benhuang.baobaobluetoothprinter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import taobe.tec.jcc.JChineseConvertor;

public class MainActivity extends AppCompatActivity implements Runnable {

    protected static final String TAG = "MainActivity";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private RecyclerView mRecyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    MyListAdapter myListAdapter;
    private ArrayList<HashMap<String,String>> orderArrayList = new ArrayList<>();
    private List<HashMap<String, String>> datas = new ArrayList<>();
    Button mGetPrintedOrdersBtn, mGetUnPrintedOrdersBtn, mScan, mPrint;
    TextView stat;

    BluetoothAdapter mBluetoothAdapter;
    private UUID applicationUUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ProgressDialog mBluetoothConnectProgressDialog;
    private BluetoothSocket mBluetoothSocket;
    BluetoothDevice mBluetoothDevice;

    /* Get time and date */
    Calendar c = Calendar.getInstance();
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US);
    final String formattedDate = df.format(c.getTime());

    // 建立OkHttpClient
    OkHttpClient client = new OkHttpClient().newBuilder().build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        getUnPrintedOrder();

        //設置RecycleView
        mRecyclerView = findViewById(R.id.recycleview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        myListAdapter = new MyListAdapter(MainActivity.this, datas);
        mRecyclerView.setAdapter(myListAdapter);
        //下拉刷新
        swipeRefreshLayout = findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.blue_RURI));
        swipeRefreshLayout.setOnRefreshListener(()->{
            datas.clear();
            orderArrayList.clear();
            getUnPrintedOrder();
            myListAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                freshHandler.sendEmptyMessage(1);
                freshHandler.postDelayed(this, 60*1000);
            }
        });


        //藍芽
        stat = findViewById(R.id.bpstatus);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mScan = findViewById(R.id.Scan);
        mScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View mView) {

                if (mScan.getText().equals("Connect")) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter == null) {
                        Toast.makeText(MainActivity.this, "Message1", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            ListPairedDevices();
                            Intent connectIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                            startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                        }
                    }

                } else if (mScan.getText().equals("Disconnect")) {
                    if (mBluetoothAdapter != null)
                        mBluetoothAdapter.disable();
                    stat.setText("");
                    stat.setText("Disconnected");
                    stat.setTextColor(Color.rgb(199, 59, 59));
                    mPrint.setEnabled(false);
                    mScan.setEnabled(true);
                    mScan.setText("Connect");
                }

            }
        });

        mGetUnPrintedOrdersBtn = findViewById(R.id.get_unprinted_orders_btn);
        mGetPrintedOrdersBtn = findViewById(R.id.get_printed_orders_btn);

        mGetUnPrintedOrdersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datas.clear();
                orderArrayList.clear();
                getUnPrintedOrder();
                myListAdapter.notifyDataSetChanged();
            }
        });
        mGetPrintedOrdersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datas.clear();
                orderArrayList.clear();
                getPrintedOrders();
                myListAdapter.notifyDataSetChanged();
            }
        });

        mPrint = findViewById(R.id.mPrint);
        mPrint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View mView) {
                printContext("TEST" );
                Log.d("mPrint result", "TEST");
            }
        });
    }

    public void printContext(final String ss) {
        Thread t = new Thread() {
            public void run() {
                try {

                    OutputStream os = mBluetoothSocket.getOutputStream();
                    String header = "";
                    String he = "";
                    String blank = "";
                    String header2 = "";
                    String BILL = "";
                    String vio = "";
                    String header3 = "";
                    String mvdtail = "";
                    String header4 = "";
                    String offname = "";
                    String time = "";
                    String copy = "";
                    String checktop_status = "";

                    JChineseConvertor jChineseConvertor = JChineseConvertor.getInstance();

                    blank = "\n\n";
                    he = "      饱饱铺订单\n";
                    he = he + "********************************\n\n";

                    header = "订购人:\n";
                    BILL += ss.toString() + "\n";
                    BILL = jChineseConvertor.t2s(BILL)
                            + "================================\n";
                    header2 = "位置:\n";
                    vio = ss.toString() + "\n";
                    vio = jChineseConvertor.t2s(vio)
                            + "================================\n";
                    header3 = "订单:\n";
                    mvdtail = ss.toString() + "\n";
                    mvdtail = jChineseConvertor.t2s(mvdtail)
                            + "================================\n";
                    time = formattedDate + "\n\n";
                    copy = "-Customer's Copy\n\n\n\n\n\n\n\n\n";

                    os.write(blank.getBytes("GB2312"));
                    os.write(he.getBytes("GB2312"));
                    os.write(header.getBytes("GB2312"));
                    os.write(BILL.getBytes("GB2312"));
                    os.write(header2.getBytes("GB2312"));
                    os.write(vio.getBytes("GB2312"));
                    os.write(header3.getBytes("GB2312"));
                    os.write(mvdtail.getBytes("GB2312"));
                    os.write(header4.getBytes("GB2312"));
                    os.write(offname.getBytes("GB2312"));
                    os.write(checktop_status.getBytes("GB2312"));
                    os.write(time.getBytes("GB2312"));
                    os.write(copy.getBytes("GB2312"));

                    // Setting height
                    int gs = 29;
                    os.write(intToByteArray(gs));
                    int h = 150;
                    os.write(intToByteArray(h));
                    int n = 170;
                    os.write(intToByteArray(n));

                    // Setting Width
                    int gs_width = 29;
                    os.write(intToByteArray(gs_width));
                    int w = 119;
                    os.write(intToByteArray(w));
                    int n_width = 2;
                    os.write(intToByteArray(n_width));

                } catch (Exception e) {
                    Log.e("PrintActivity", "Exe ", e);
                }
            }
        };
        t.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* Terminate bluetooth connection and close all sockets opened */
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (Exception e) {
            Log.e("Tag", "Exe ", e);
        }
    }

    public void onActivityResult(int mRequestCode, int mResultCode, Intent mDataIntent) {
        super.onActivityResult(mRequestCode, mResultCode, mDataIntent);

        switch (mRequestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (mResultCode == Activity.RESULT_OK) {
                    Bundle mExtra = mDataIntent.getExtras();
                    String mDeviceAddress = mExtra.getString("DeviceAddress");
                    Log.v(TAG, "Coming incoming address " + mDeviceAddress);
                    mBluetoothDevice = mBluetoothAdapter
                            .getRemoteDevice(mDeviceAddress);
                    mBluetoothConnectProgressDialog = ProgressDialog.show(this,
                            "Connecting...", mBluetoothDevice.getName() + " : "
                                    + mBluetoothDevice.getAddress(), true, false);
                    Thread mBlutoothConnectThread = new Thread(this);
                    mBlutoothConnectThread.start();
                    // pairToDevice(mBluetoothDevice); This method is replaced by
                    // progress dialog with thread
                }
                break;

            case REQUEST_ENABLE_BT:
                if (mResultCode == Activity.RESULT_OK) {
                    ListPairedDevices();
                    Intent connectIntent = new Intent(MainActivity.this,
                            DeviceListActivity.class);
                    startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to any device", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //列出所有配對出單機
    private void ListPairedDevices() {
        Set<BluetoothDevice> mPairedDevices = mBluetoothAdapter
                .getBondedDevices();
        if (mPairedDevices.size() > 0) {
            for (BluetoothDevice mDevice : mPairedDevices) {
                Log.v(TAG, "PairedDevices: " + mDevice.getName() + "  "
                        + mDevice.getAddress());
            }
        }
    }

    //出單
    public void run() {
        try {
            mBluetoothSocket = mBluetoothDevice
                    .createRfcommSocketToServiceRecord(applicationUUID);
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothSocket.connect();
            mHandler.sendEmptyMessage(0);
        } catch (IOException eConnectException) {
            Log.d(TAG, "CouldNotConnectToSocket", eConnectException);
            closeSocket(mBluetoothSocket);
            return;
        }
    }

    //關閉Socket
    private void closeSocket(BluetoothSocket nOpenSocket) {
        try {
            nOpenSocket.close();
            Log.d(TAG, "SocketClosed");
        } catch (IOException ex) {
            Log.d(TAG, "CouldNotCloseSocket");
        }
    }

    // 連線或是斷線文字呈現
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mBluetoothConnectProgressDialog.dismiss();

            stat.setText("");
            stat.setText("Connected");
            stat.setTextColor(Color.rgb(97, 170, 74));
            mPrint.setEnabled(true);
            mScan.setText("Disconnect");
        }
    };

    public static byte intToByteArray(int value) {
        byte[] b = ByteBuffer.allocate(4).putInt(value).array();

        for (int k = 0; k < b.length; k++) {
            System.out.println("Selva  [" + k + "] = " + "0x"
                    + UnicodeFormatter.byteToHex(b[k]));
        }
        return b[3];
    }

    //Init
    private Handler initHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    makeData(datas);
                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                    mRecyclerView.setAdapter(new MyListAdapter(MainActivity.this, datas));
                    break;
            }
        }
    };

    private Handler freshHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    //設置RecycleView
                    mRecyclerView = findViewById(R.id.recycleview);
                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                    myListAdapter = new MyListAdapter(MainActivity.this, datas);
                    mRecyclerView.setAdapter(myListAdapter);
                    datas.clear();
                    orderArrayList.clear();
                    getUnPrintedOrder();
                    myListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private void initView() {
        mRecyclerView = findViewById(R.id.recycleview);
    }

    private void getUnPrintedOrder() {

        // 建立Request，設置連線資訊
        Request request = new Request.Builder()
                .url("https://itioi.com/api/orders")
                .build();
        // 建立Call
        Call call = client.newCall(request);

        // 執行Call連線到網址
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 連線成功，自response取得連線結果
                String result = response.body().string();

                Gson gson = new Gson();
                Orders[] orderArray = gson.fromJson(result, Orders[].class);

                for (Orders order : orderArray)
                {
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put("OrderId", order.getOrderId());
                    hashMap.put("OrderName", order.getOrderName());
                    hashMap.put("OrderAddress", order.getOrderAddress());
                    hashMap.put("OrderMobile", order.getOrderMobile());
                    hashMap.put("Orders", order.getOrders());
                    hashMap.put("OrderStatus", order.getOrderStatus());
                    hashMap.put("OrderCreatedAt", order.getOrderCreatedAt());

                    datas.add(hashMap);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful())
                        {
                            initHandler.sendEmptyMessage(1);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {}
        });
    }

    private void getPrintedOrders() {

        // 建立Request，設置連線資訊
        Request request = new Request.Builder()
                .url("https://itioi.com/api/orders/printed")
                .build();
        // 建立Call
        Call call = client.newCall(request);

        // 執行Call連線到網址
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 連線成功，自response取得連線結果
                String result = response.body().string();

                Gson gson = new Gson();
                Orders[] orderArray = gson.fromJson(result, Orders[].class);

                for (Orders order : orderArray)
                {
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put("OrderId", order.getOrderId());
                    hashMap.put("OrderName", order.getOrderName());
                    hashMap.put("OrderAddress", order.getOrderAddress());
                    hashMap.put("OrderMobile", order.getOrderMobile());
                    hashMap.put("Orders", order.getOrders());
                    hashMap.put("OrderStatus", order.getOrderStatus());
                    hashMap.put("OrderCreatedAt", order.getOrderCreatedAt());

                    datas.add(hashMap);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful())
                        {
                            initHandler.sendEmptyMessage(1);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {}
        });
    }

    private void makeData(@NonNull List<HashMap<String, String>> data) {
        for (int i = 0; i < data.size();i++){
            HashMap<String,String> hashMap = new HashMap<>();

            hashMap.put("Id","訂單：" + data.get(i).get("OrderId"));
            hashMap.put("Customer", data.get(i).get("OrderName"));
            hashMap.put("Address", data.get(i).get("OrderAddress"));
            hashMap.put("Mobile", data.get(i).get("OrderMobile"));
            hashMap.put("Orders", data.get(i).get("Orders"));

            orderArrayList.add(hashMap);
        }
    }

    private class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder>{

        public MyListAdapter(MainActivity mainActivity, List<HashMap<String, String>> datas) {}

        class ViewHolder extends RecyclerView.ViewHolder{
            private TextView txtItem, printOrderId,printOrderName,
                    printOrderAddress,printOrders,printOrderMobile;
            private View mView;
            private Button btnPrint;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                printOrderId = itemView.findViewById(R.id.printOrderId);
                printOrderName = itemView.findViewById(R.id.printOrderName);
                printOrderAddress = itemView.findViewById(R.id.printOrderAddress);
                printOrderMobile = itemView.findViewById(R.id.printOrderMobile);
                printOrders  = itemView.findViewById(R.id.printOrders);
                mView  = itemView;

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
            holder.printOrderId.setText(orderArrayList.get(position).get("Id"));
            holder.printOrderName.setText(orderArrayList.get(position).get("Customer"));
            holder.printOrderAddress.setText(orderArrayList.get(position).get("Address"));
            holder.printOrderMobile.setText(orderArrayList.get(position).get("Mobile"));
            holder.printOrders.setText(orderArrayList.get(position).get("Orders"));

            holder.mView.setOnClickListener((v)->{
                Toast.makeText(getBaseContext(),holder.txtItem.getText(),Toast.LENGTH_SHORT).show();
            });

            holder.btnPrint.setOnClickListener((v)->{
                freshHandler.sendEmptyMessage(1);

                printContext(orderArrayList.get(position).get("Customer"));
            });
        }

        @Override
        public int getItemCount() {
            return orderArrayList.size();
        }
    }
}