package com.benhuang.baobaobluetoothprinter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import taobe.tec.jcc.JChineseConvertor;

public class MainActivity extends AppCompatActivity implements Runnable {

    public static final int LOGIN_RESULT = 3;
    public String mAccount = "NOSETTING";
    boolean isLogin = false;
    protected static final String TAG = "MainActivity";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private RecyclerView mRecyclerView;
    private TextView emptyView;

    SwipeRefreshLayout swipeRefreshLayout;
    MyListAdapter myListAdapter;
    private ArrayList<HashMap<String,String>> orderArrayList = new ArrayList<>();
    private List<HashMap<String, String>> datas = new ArrayList<>();

    private ArrayList<HashMap<String,String>> unprintedOrderArrayList = new ArrayList<>();
    private ArrayList<HashMap<String,String>> printedOrderArrayList = new ArrayList<>();
    private List<HashMap<String, String>> unprintedDatas = new ArrayList<>();
    private List<HashMap<String, String>> printedDatas = new ArrayList<>();
    private ArrayList<String> unprintedOrderId = new ArrayList<>();
    private ArrayList<String> printedOrderId = new ArrayList<>();

    Button mGetPrintedOrdersBtn, mGetUnPrintedOrdersBtn, mGetLoginBtn, mScan, mPrint;
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
    OkHttpClient client = new OkHttpClient()
                            .newBuilder()
                            .build();

    boolean unprinted_orders_page = false;
    boolean printed_orders_page = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //設置RecycleView
        mRecyclerView = findViewById(R.id.recycleview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        myListAdapter = new MyListAdapter(MainActivity.this, datas);
        mRecyclerView.setAdapter(myListAdapter);

        emptyScreen();

        //下拉刷新
        swipeRefreshLayout = findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.blue_RURI));
        swipeRefreshLayout.setOnRefreshListener(()->{

            unprinted_orders_page = true;
            printed_orders_page = false;

            mGetUnPrintedOrdersBtn.setBackgroundColor(Color.parseColor("#F65D21"));
            mGetPrintedOrdersBtn.setBackgroundColor(Color.parseColor("#F0C362"));
            mGetLoginBtn.setBackgroundColor(Color.parseColor("#F0C362"));

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
                if (orderArrayList.size() != 0)
                {
                    final MediaPlayer dingSound = MediaPlayer.create(MainActivity.this, R.raw.notification);
                    dingSound.start();
                }
                freshHandler.postDelayed(this, 10*1000);
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
        mGetLoginBtn = findViewById(R.id.get_login_btn);

        mGetUnPrintedOrdersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                unprinted_orders_page = true;
                printed_orders_page = false;

                mGetUnPrintedOrdersBtn.setBackgroundColor(Color.parseColor("#F65D21"));
                mGetPrintedOrdersBtn.setBackgroundColor(Color.parseColor("#F0C362"));
                mGetLoginBtn.setBackgroundColor(Color.parseColor("#F0C362"));

                datas.clear();
                orderArrayList.clear();
                getUnPrintedOrder();
                myListAdapter.notifyDataSetChanged();
            }
        });

        mGetPrintedOrdersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                printed_orders_page = true;
                unprinted_orders_page = false;

                mGetUnPrintedOrdersBtn.setBackgroundColor(Color.parseColor("#F0C362"));
                mGetPrintedOrdersBtn.setBackgroundColor(Color.parseColor("#F65D21"));
                mGetLoginBtn.setBackgroundColor(Color.parseColor("#F0C362"));

                datas.clear();
                orderArrayList.clear();
                getPrintedOrders();

                myListAdapter.notifyDataSetChanged();
            }
        });

        mGetLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printed_orders_page = false;
                unprinted_orders_page = false;

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(intent, LOGIN_RESULT);
            }
        });

        mPrint = findViewById(R.id.mPrint);
        mPrint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View mView) {
                printContextDemo();
                Log.d("mPrint result", "TEST");
            }
        });
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

            case LOGIN_RESULT:
                if (mResultCode == Activity.RESULT_OK) {
                    Bundle mExtra = mDataIntent.getExtras();
                    mAccount = mExtra.getString("account");
                    Log.v(TAG, "mAccount " + mAccount);
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

    public void emptyScreen() {
        emptyView = (TextView) findViewById(R.id.empty_view);

        if (myListAdapter.getItemCount() != 0) {
            mRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
        else {
            mRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    //Init
    private Handler initHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    mGetUnPrintedOrdersBtn.setText("最新訂單("+ datas.size() + ")");

                    makeData(datas);

                    makeUnprintedData(unprintedDatas);

                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                    mRecyclerView.setAdapter(new MyListAdapter(MainActivity.this, datas));
                    emptyScreen();
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
                    if (isLogin == true)
                    {
                        findViewById(R.id.loadingProgressBar).setVisibility(View.VISIBLE);
                    }

                    mRecyclerView = findViewById(R.id.recycleview);
                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                    myListAdapter = new MyListAdapter(MainActivity.this, datas);
                    mRecyclerView.setAdapter(myListAdapter);
                    datas.clear();
                    orderArrayList.clear();

                    if (unprinted_orders_page == true){
                        getUnPrintedOrder();
                    } else {
                        getPrintedOrders();
                    }

                    myListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private void initView() {
        mRecyclerView = findViewById(R.id.recycleview);
    }

    private void getUnPrintedOrder() {

        if (mAccount != "NOSETTING")
        {
            // 建立Request，設置連線資訊
            Request request = new Request.Builder()
                    .url("https://admin.baobaopuo.com/admin/api/orders?print_status=unprinted")
                    .addHeader("api-key", mAccount)
                    .build();
            // 建立Call
            Call call = client.newCall(request);

            // 執行Call連線到網址
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    try {
                        // 連線成功，自response取得連線結果

                        isLogin = true;

                        String result = response.body().string();
                        Gson gson = new Gson();
                        Orders[] orderArray = gson.fromJson(result, Orders[].class);

                        for (Orders order : orderArray)
                        {
                            HashMap<String,String> hashMap = new HashMap<>();

                            hashMap.put("OrderId", order.getOrderId());
                            hashMap.put("OrderNum", order.getOrderNum());
                            hashMap.put("OrderName", order.getOrderName());
                            hashMap.put("OrderAddress", order.getOrderAddress());
                            hashMap.put("OrderMobile", order.getOrderMobile());
                            hashMap.put("Orders", order.getOrders());
                            hashMap.put("OrderDiscount", order.getDiscount());
                            hashMap.put("OrderTotal", order.getTotal());
                            hashMap.put("OrderTableware", order.getTableware());
                            hashMap.put("OrderStatus", order.getOrderStatus());
                            hashMap.put("OrderCreatedAt", order.getOrderCreatedAt());

                            if (!unprintedOrderId.contains(order.getOrderId())){
                                unprintedOrderId.add(order.getOrderId());
                                unprintedDatas.add(hashMap);
                            }

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
                    catch (Throwable e) {}
                }

                @Override
                public void onFailure(Call call, IOException e) {}
            });

        }
    }

    private void getPrintedOrders() {

        if (mAccount != "NOSETTING")
        {
            // 建立Request，設置連線資訊
            Request request = new Request.Builder()
                    .url("https://admin.baobaopuo.com/admin/api/orders?print_status=printed")
                    .addHeader("api-key", mAccount)
                    .build();
            // 建立Call
            Call call = client.newCall(request);

            // 執行Call連線到網址
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    try {
                        // 連線成功，自response取得連線結果
                        String result = response.body().string();

                        Gson gson = new Gson();
                        Orders[] orderArray = gson.fromJson(result, Orders[].class);

                        for (Orders order : orderArray)
                        {
                            HashMap<String,String> hashMap = new HashMap<>();

                            hashMap.put("OrderId", order.getOrderId());
                            hashMap.put("OrderNum", order.getOrderNum());
                            hashMap.put("OrderName", order.getOrderName());
                            hashMap.put("OrderAddress", order.getOrderAddress());
                            hashMap.put("OrderMobile", order.getOrderMobile());
                            hashMap.put("Orders", order.getOrders());
                            hashMap.put("OrderDiscount", order.getDiscount());
                            hashMap.put("OrderTotal", order.getTotal());
                            hashMap.put("OrderTableware", order.getTableware());
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
                    catch (Throwable e){}
                }

                @Override
                public void onFailure(Call call, IOException e) {}
            });
        }

    }

    private void updateOrder(String passId) {

        if (mAccount != "NOSETTING")
        {
            FormBody formBody = new FormBody.Builder()
                    .add("oid", passId)
                    .add("status", "printed")
                    .build();

            // 建立Request，設置連線資訊
            Request request = new Request.Builder()
                    .url("https://admin.baobaopuo.com/admin/api/update-print-status")
                    .addHeader("api-key", mAccount)
                    .post(formBody)
                    .build();
            // 建立Call
            Call call = client.newCall(request);

            // 執行Call連線到網址
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
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

    }

    private void cancelOrder(String passId) {

        if (mAccount != "NOSETTING")
        {
            FormBody formBody = new FormBody.Builder()
                    .add("oid", passId)
                    .add("status", "cancel")
                    .build();

            // 建立Request，設置連線資訊
            Request request = new Request.Builder()
                    .url("https://admin.baobaopuo.com/admin/api/update-delivery-status")
                    .addHeader("api-key", mAccount)
                    .post(formBody)
                    .build();
            // 建立Call
            Call call = client.newCall(request);

            // 執行Call連線到網址
            call.enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
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

    }

    private void makeData(@NonNull List<HashMap<String, String>> data) {
        for (int i = 0; i < data.size();i++){
            HashMap<String,String> hashMap = new HashMap<>();

            hashMap.put("Id", data.get(i).get("OrderId"));
            hashMap.put("No", data.get(i).get("OrderNum"));
            hashMap.put("Customer", data.get(i).get("OrderName"));
            hashMap.put("Address", data.get(i).get("OrderAddress"));
            hashMap.put("Mobile", data.get(i).get("OrderMobile"));
            hashMap.put("Orders", data.get(i).get("Orders"));
            hashMap.put("Discount", data.get(i).get("OrderDiscount"));
            hashMap.put("Total", data.get(i).get("OrderTotal"));
            hashMap.put("Tableware", data.get(i).get("OrderTableware"));
            hashMap.put("Status", data.get(i).get("OrderStatus"));
            hashMap.put("CreatedAt", data.get(i).get("OrderCreatedAt"));

            orderArrayList.add(hashMap);
        }

        findViewById(R.id.loadingProgressBar).setVisibility(View.GONE);
    }

    private void makeUnprintedData(@NonNull List<HashMap<String, String>> data) {
        for (int i = 0; i < data.size();i++){
            HashMap<String,String> hashMap = new HashMap<>();

            hashMap.put("Id", data.get(i).get("OrderId"));
            hashMap.put("No", data.get(i).get("OrderNum"));
            hashMap.put("Customer", data.get(i).get("OrderName"));
            hashMap.put("Address", data.get(i).get("OrderAddress"));
            hashMap.put("Mobile", data.get(i).get("OrderMobile"));
            hashMap.put("Orders", data.get(i).get("Orders"));
            hashMap.put("Discount", data.get(i).get("OrderDiscount"));
            hashMap.put("Total", data.get(i).get("OrderTotal"));
            hashMap.put("Tableware", data.get(i).get("OrderTableware"));
            hashMap.put("Status", data.get(i).get("OrderStatus"));
            hashMap.put("CreatedAt", data.get(i).get("OrderCreatedAt"));

            unprintedOrderArrayList.add(hashMap);
        }

        findViewById(R.id.loadingProgressBar).setVisibility(View.GONE);
    }

    public void printContext(ArrayList<HashMap<String,String>> req, int position, String owner) {
        Thread t = new Thread() {
            public void run() {
                try {

                    OutputStream os = mBluetoothSocket.getOutputStream();
                    String blank = "";
                    String header1 = "";
                    String header2 = "";
                    String customer = "";
                    String header3 = "";
                    String address = "";
                    String header4 = "";
                    String orders = "";
                    String header5 = "";
                    String mobile = "";
                    String header6 = "";
                    String discount = "";
                    String header7 = "";
                    String total = "";
                    String header9 = "";
                    String createat = "";
                    String header10 = "";
                    String tableware = "";
                    String header11 = "";
                    String NO = "";
                    String footer = "";

                    blank = "\n\n";
                    header1 = "       BAO BAO PUO 【"+owner+"】\n";
                    header1 = header1 + "********************************\n\n";

                    header2 = "客戶名稱:\n";
                    customer += req.get(position).get("Customer") + "\n";
                    customer += "--------------------------------\n";

                    header3 = "地址:\n";
                    address = req.get(position).get("Address") + "\n";
                    address += "--------------------------------\n";

                    header4 = "訂單明細:\n";

                    String reqOrder = req.get(position).get("Orders");
                    orders += "\n";
                    String[] buff = reqOrder.split(" \\| ");
                    for(int i = 0; i < buff.length; i++){
                        String[] tmpBuff = buff[i].split("X");
                        orders += tmpBuff[0] + "-----------X";
                        orders += tmpBuff[1] + "\n";
                    }
                    orders += "--------------------------------\n";

                    header5 = "聯絡電話:\n";
                    mobile = req.get(position).get("Mobile") + "\n";
                    mobile += "--------------------------------\n";

                    header6 = "折扣金額:\n";
                    discount = req.get(position).get("Discount") + "\n";
                    discount += "--------------------------------\n";

                    header7 = "總金額:\n";
                    total = req.get(position).get("Total") + "\n";
                    total += "--------------------------------\n";

                    header9 = "時間:\n";
                    createat = req.get(position).get("CreatedAt") + "\n";
                    createat += "--------------------------------\n";

                    String Tableware2CH = req.get(position).get("Tableware");
                    if (Tableware2CH == "true")
                    {
                        Tableware2CH = "需要";
                    }else {
                        Tableware2CH = "不需要";
                    }

                    header10 = "是否需要餐具:\n";
                    tableware = Tableware2CH + "\n";
                    tableware += "--------------------------------\n";

                    header11 = "No:\n";
                    NO = req.get(position).get("No") + "\n";
                    NO += "--------------------------------\n";

                    footer = "        BAO BAO PUO CO.\n\n\n\n\n\n\n\n\n";

                    byte[] fontstyle = new byte[]{0x1B,0x21,0x06}; // normal
                    os.write(fontstyle);

                    os.write(blank.getBytes("GBK"));
                    os.write(header1.getBytes("GBK"));

                    os.write(header11.getBytes("GBK"));
                    os.write(NO.getBytes("GBK"));

                    fontstyle = new byte[]{0x1B,0x16,0x12}; // 3- bold with large text
                    os.write(fontstyle);

                    os.write(header9.getBytes("GBK"));
                    os.write(createat.getBytes("GBK"));

                    os.write(header2.getBytes("GBK"));
                    os.write(customer.getBytes("GBK"));

                    os.write(header3.getBytes("GBK"));
                    os.write(address.getBytes("GBK"));

                    fontstyle = new byte[]{0x1B,0x21,0x06}; // normal
                    os.write(fontstyle);

                    os.write(header4.getBytes("GBK"));
                    os.write(orders.getBytes("GBK"));

                    os.write(header5.getBytes("GBK"));
                    os.write(mobile.getBytes("GBK"));

                    os.write(header6.getBytes("GBK"));
                    os.write(discount.getBytes("GBK"));

                    os.write(header7.getBytes("GBK"));
                    os.write(total.getBytes("GBK"));

                    os.write(header10.getBytes("GBK"));
                    os.write(tableware.getBytes("GBK"));

                    os.write(footer.getBytes("GBK"));

                    // 切紙ESC指令
                    os.write(new byte[]{ 0x1D,
                            0x56,
                            66,
                            0x00});

                    /*
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
                    */

                } catch (Exception e) {
                    Log.e("PrintActivity", "Exe ", e);
                }
            }
        };
        t.start();
    }

    public void printContextDemo() {
        Thread t = new Thread() {
            public void run() {
                try {

                    OutputStream os = mBluetoothSocket.getOutputStream();
                    String blank = "";
                    String header1 = "";
                    String header2 = "";
                    String customer = "";
                    String header3 = "";
                    String address = "";
                    String header4 = "";
                    String orders = "";
                    String header5 = "";
                    String mobile = "";
                    String header6 = "";
                    String discount = "";
                    String header7 = "";
                    String total = "";
                    String header8 = "";
                    String status = "";
                    String header9 = "";
                    String createat = "";
                    String footer = "";

                    blank = "\n\n";
                    header1 = "      BaoBaoPu Order\n";
                    header1 = header1 + "********************************\n\n";

                    header2 = "訂購人:\n";
                    customer += "海綿寶寶" + "\n";
                    customer += "--------------------------------\n";

                    header3 = "位置:\n";
                    address = "比奇堡" + "\n";
                    address += "--------------------------------\n";

                    header4 = "訂單:\n";
                    orders = "美味蟹堡X2 ----------- 300" + "\n";
                    orders += "美味熱狗X5 ----------- 500" + "\n";
                    orders += "--------------------------------\n";

                    header5 = "電話:\n";
                    mobile = "28825252" + "\n";
                    mobile += "--------------------------------\n";

                    header6 = "折扣:\n";
                    discount = "0" + "\n";
                    discount += "--------------------------------\n";

                    header7 = "總金額:\n";
                    total = "300元" + "\n";
                    total += "--------------------------------\n";

                    header8 = "狀態:\n";
                    status = "TEST" + "\n";
                    status += "--------------------------------\n";

                    header9 = "時間:\n";
                    createat = formattedDate + "\n";
                    createat += "--------------------------------\n";


                    footer = "         BaoBaoPu CO.\n\n\n\n\n\n\n\n\n";

                    os.write(blank.getBytes("GBK"));
                    os.write(header1.getBytes("GBK"));

                    byte[] fontstyle = new byte[]{0x1B,0x21,0x16}; // 3- bold with large text
                    os.write(fontstyle);

                    os.write(header2.getBytes("GBK"));
                    os.write(customer.getBytes("GBK"));
                    os.write(header3.getBytes("GBK"));
                    os.write(address.getBytes("GBK"));
                    fontstyle = new byte[]{0x1B,0x21,0x06}; // 3- bold with large text
                    os.write(fontstyle);
                    os.write(header4.getBytes("GBK"));
                    os.write(orders.getBytes("GBK"));
                    os.write(header5.getBytes("GBK"));
                    os.write(mobile.getBytes("GBK"));
                    os.write(header6.getBytes("GBK"));
                    os.write(discount.getBytes("GBK"));
                    os.write(header7.getBytes("GBK"));
                    os.write(total.getBytes("GBK"));
                    os.write(header8.getBytes("GBK"));
                    os.write(status.getBytes("GBK"));
                    os.write(header9.getBytes("GBK"));
                    os.write(createat.getBytes("GBK"));
                    os.write(footer.getBytes("GBK"));

                    // 切紙ESC指令
                    os.write(new byte[]{ 0x1D,
                            0x56,
                            66,
                            0x00});

                } catch (Exception e) {
                    Log.e("PrintActivity", "Exe ", e);
                }
            }
        };
        t.start();
    }

    private class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder>{

        public MyListAdapter(MainActivity mainActivity, List<HashMap<String, String>> datas) {}

        class ViewHolder extends RecyclerView.ViewHolder{
            private TextView txtItem, printOrderId,printOrderName, printOrderMobile,
                    printOrderAddress,printOrders, printDiscount, printTotal, printStatus, printTableware ,printCreateAt;
            private View mView;
            private Button btnPrint, btnCancel;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                printOrderId = itemView.findViewById(R.id.printOrderId);
                printOrderName = itemView.findViewById(R.id.printOrderName);
                printOrderAddress = itemView.findViewById(R.id.printOrderAddress);
                printOrderMobile = itemView.findViewById(R.id.printOrderMobile);
                printOrders  = itemView.findViewById(R.id.printOrders);
                printDiscount = itemView.findViewById(R.id.printDiscount);
                printTotal = itemView.findViewById(R.id.printTotal);
                printTableware = itemView.findViewById(R.id.printTableware);
                printStatus = itemView.findViewById(R.id.printStatus);
                printCreateAt = itemView.findViewById(R.id.printCreateAt);

                mView  = itemView;

                txtItem = (TextView) itemView.findViewById(R.id.printOrderMobile);
                btnPrint = (Button) itemView.findViewById(R.id.printBtn);
                btnCancel = (Button) itemView.findViewById(R.id.cancelBtn);

                if(printed_orders_page == true) {
                    btnPrint.setText("再次列印");
                }
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
            holder.printOrderId.setText(orderArrayList.get(position).get("No"));
            holder.printOrderName.setText(orderArrayList.get(position).get("Customer"));
            holder.printOrderAddress.setText(orderArrayList.get(position).get("Address"));
            holder.printOrderMobile.setText(orderArrayList.get(position).get("Mobile"));
            holder.printOrders.setText(orderArrayList.get(position).get("Orders"));
            holder.printDiscount.setText(orderArrayList.get(position).get("Discount"));
            holder.printTotal.setText(orderArrayList.get(position).get("Total"));
            holder.printTableware.setText(orderArrayList.get(position).get("Tableware"));
            holder.printStatus.setText(orderArrayList.get(position).get("Status"));
            holder.printCreateAt.setText(orderArrayList.get(position).get("CreatedAt"));

//            holder.mView.setOnClickListener((v)->{
//                Toast.makeText(getBaseContext(),holder.txtItem.getText(),Toast.LENGTH_SHORT).show();
//            });

            // 出單
            holder.btnPrint.setOnClickListener((v)->{

                String id = orderArrayList.get(position).get("Id").toString();
                printContext(orderArrayList, position, "IN");
                updateOrder(id);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        printContext(orderArrayList, position, "OUT");
                        freshHandler.sendEmptyMessage(1);
                    }
                }, 1500);



            });

            // 取消訂單
            holder.btnCancel.setOnClickListener((v)->{
                String id = orderArrayList.get(position).get("Id").toString();
                cancelOrder(id);
                freshHandler.sendEmptyMessage(1);
            });
        }

        @Override
        public int getItemCount() {
            return orderArrayList.size();
        }
    }
}