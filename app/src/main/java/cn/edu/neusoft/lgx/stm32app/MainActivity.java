package cn.edu.neusoft.lgx.stm32app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ThemedSpinnerAdapter;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.neusoft.qiangzi.bluetoothservicedemo.BluetoothManager;
import com.neusoft.qiangzi.bluetoothservicedemo.IOnBluetoothListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private TextView info,temShow,temMax,temMin,temContorl,humShow,humMax,humMin,humControl,timeshow;
    private ImageView blueStatic;
    private final String TAG = "MainActivity";
    private DataBaseHelp dataBaseHelp;
    private OperateTable operateTable;
    private SearchTable searchTable;
    private Handler handler;
    private Datalist datalist = new Datalist();
    private DynamicLineChartManager dynamicLineChartManager;
    private boolean isRuleRefresh = true;


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        blueStatic = findViewById(R.id.imageView_bluetoothStatic);
        info = findViewById(R.id.textView_info);



        temShow = findViewById(R.id.textView_temShow);
        temMax = findViewById(R.id.textView_temMax);
        temMin = findViewById(R.id.textView_temMin);
        temContorl = findViewById(R.id.textView_temControl);
        humShow = findViewById(R.id.textView_humShow);
        humMax = findViewById(R.id.textView_humMax);
        humMin = findViewById(R.id.textView_humMin);
        humControl = findViewById(R.id.textView_humControl);
        timeshow = findViewById(R.id.textView_timeShow);
        LineChart mChart2 = (LineChart) findViewById(R.id.dynamic_chart2);

        dataBaseHelp = new DataBaseHelp(this);

        //创建服务管理器对象
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.setBluetoothListener(receivedListener);
        bluetoothManager.setOnBindedListener(new BluetoothManager.OnBindedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onBinded() {
                //Log.e(TAG, "onBinded: 调用蓝牙回调" );
                if (bluetoothManager.isConnected()) {
                    info.setText("蓝牙已连接：" + bluetoothManager.getRemoteName());
                    blueStatic.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24);
                } else {
                    info.setText("蓝牙未连接");
                    blueStatic.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
                }
            }
        });

        //曲线图设定
        dynamicLineChartManager = new DynamicLineChartManager(mChart2, Arrays.asList("温度","湿度"), Arrays.asList(Color.RED,Color.BLUE));
        dynamicLineChartManager.setYAxis(100, 0, 10);

        //画面刷新
        handler = new Handler(){
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                showRefresh(datalist);
            }
        };

        //时间刷新
        timeThreed.run();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bluetoothManager.isStared()) {
            //Log.e(TAG, "onStart: try bind Bluetooth");
            bluetoothManager.bind();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothManager.unbind();
    }

    public void setButtonClick(View view){
        isRuleRefresh = true;
    }

    public void blueButtonClick(View view){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.neusoft.qiangzi.bluetoothservicedemo","com.neusoft.qiangzi.bluetoothservicedemo.MainActivity"));
        try {
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this,"跳转蓝牙失败",Toast.LENGTH_SHORT).show();
        }
    }

    //创建服务事件监听器对象，用于接收服务的主动通知
    IOnBluetoothListener receivedListener = new IOnBluetoothListener.Stub() {
        //蓝牙数据通过这个函数传给activity
        @Override
        public void onReceived(String key, String value) throws RemoteException {
            value = value.trim();
            Log.e(TAG, "onReceived: bluetooth get"+value+" key: "+key );
            if(key.equals("dht11Get"))
                dataRefresh(value);
        }

        @Override
        public void onEvent(int e) throws RemoteException {

        }
    };

    private void dataRefresh(String data){
        String[] datas = data.split(",");
        if (datas.length != 2)
            return;
        datalist.time = TimeGet.GetTime();
        datalist.tem = Integer.parseInt(datas[0]);
        datalist.hum = Integer.parseInt(datas[1]);
        searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
        datalist = searchTable.getSet(datalist);
        wornGet(datalist);
        operateTable = new OperateTable(dataBaseHelp.getWritableDatabase());
        if(! operateTable.insert(datalist))
            Log.e(TAG, "dataRefresh: 数据存入失败!" );
        handler.sendEmptyMessage(0);
    }

    private void wornGet(Datalist data){
        int[] w = new int[4];
        if(data.tem > data.temMax)
            w[0] = 1;
        else if(data.tem < data.temMin)
            w[1] = 1;
        if(data.hum > data.humMax)
            w[2] = 1;
        else if(data.hum < data.humMin)
            w[3] = 1;
        data.worn = w[0]*8+w[1]*4+w[2]*2+w[3];
    }

    private void showRefresh(Datalist data){
        temShow.setText(String.valueOf(data.tem));
        temMax.setText(String.valueOf(data.temMax));
        temMin.setText(String.valueOf(data.temMin));
        humShow.setText(String.valueOf(data.hum));
        humMax.setText(String.valueOf(data.humMax));
        humMin.setText(String.valueOf(data.humMin));
        wornRefresh(data.worn);
        lineCharRefresh(data);
    }

    private void wornRefresh(int n){
        if (n/8 == 1)
            temContorl.setText("降温");
        else if(n/4%2 == 1)
            temContorl.setText("升温");
        else
            temContorl.setText("保持");
        if(n/2%2 == 1)
            humControl.setText("除湿");
        else if(n%2 == 1)
            humControl.setText("加湿");
        else
            temContorl.setText("保持");
    }

    private void lineCharRefresh(Datalist data){
        if(isRuleRefresh){
            dynamicLineChartManager.setHightLimitLine(data.temMax,"温度上限", Color.RED);
            dynamicLineChartManager.setHightLimitLine(data.temMin,"温度下限", Color.RED);
            dynamicLineChartManager.setHightLimitLine(data.humMax,"湿度上限", Color.BLUE);
            dynamicLineChartManager.setHightLimitLine(data.humMin,"湿度下限", Color.BLUE);
            isRuleRefresh = false;
        }

        dynamicLineChartManager.addEntry(data);
    }

    private Runnable timeThreed = new Runnable() {
        Handler timeHandler = new Handler();
        @Override
        public void run() {
            timeHandler.postDelayed(this,100);
            timeUpdate();
        }
        public void timeUpdate(){
            timeshow.setText(TimeGet.GetTime());
        }
    };

}