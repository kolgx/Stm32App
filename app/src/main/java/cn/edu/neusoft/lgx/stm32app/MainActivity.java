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

        //???????????????????????????
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.setBluetoothListener(receivedListener);
        bluetoothManager.setOnBindedListener(new BluetoothManager.OnBindedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onBinded() {
                //Log.e(TAG, "onBinded: ??????????????????" );
                if (bluetoothManager.isConnected()) {
                    info.setText("??????????????????" + bluetoothManager.getRemoteName());
                    blueStatic.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24);
                } else {
                    info.setText("???????????????");
                    blueStatic.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
                }
            }
        });

        //???????????????
        dynamicLineChartManager = new DynamicLineChartManager(mChart2, Arrays.asList("??????","??????"), Arrays.asList(Color.RED,Color.BLUE));
        dynamicLineChartManager.setYAxis(100, 0, 10);

        //????????????
        handler = new Handler(){
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                showRefresh(datalist);
            }
        };

        //????????????
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

    @Override
    protected void onRestart() {
        super.onRestart();
        isRuleRefresh = true;
    }

    public void setButtonClick(View view){
        Intent intent = new Intent(MainActivity.this,SetActivity.class);
        startActivity(intent);
    }

    public void blueButtonClick(View view){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.neusoft.qiangzi.bluetoothservicedemo","com.neusoft.qiangzi.bluetoothservicedemo.MainActivity"));
        try {
            startActivity(intent);
        }catch (Exception e){
            Toast.makeText(this,"??????????????????",Toast.LENGTH_SHORT).show();
        }
    }

    public void onRobotClick(View view){
        Toast.makeText(MainActivity.this,"????????????",Toast.LENGTH_SHORT).show();
        try {
            Intent Irobot = new Intent();
            Irobot.setComponent(new ComponentName("com.neusoft.qiangzi.baiduyuyintest","com.neusoft.qiangzi.baiduyuyintest.MainActivity"));
            startActivity(Irobot);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,"??????????????????????????????",Toast.LENGTH_SHORT).show();
        }
    }

    public void sqlitButtonClick(View view){
        Intent intent = new Intent(MainActivity.this,DataActivity.class);
        startActivity(intent);
    }

    //?????????????????????????????????????????????????????????????????????
    IOnBluetoothListener receivedListener = new IOnBluetoothListener.Stub() {
        //????????????????????????????????????activity
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
            Log.e(TAG, "dataRefresh: ??????????????????!" );
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
            temContorl.setText("??????");
        else if(n/4%2 == 1)
            temContorl.setText("??????");
        else
            temContorl.setText("??????");
        if(n/2%2 == 1)
            humControl.setText("??????");
        else if(n%2 == 1)
            humControl.setText("??????");
        else
            humControl.setText("??????");
    }

    private void lineCharRefresh(Datalist data){
        if(isRuleRefresh){
            Log.e(TAG, "lineCharRefresh: ???????????????");
            dynamicLineChartManager.removeAllLimitLine();
            dynamicLineChartManager.setHightLimitLine(data.temMax,"????????????", Color.RED);
            dynamicLineChartManager.setHightLimitLine(data.temMin,"????????????", Color.RED);
            dynamicLineChartManager.setHightLimitLine(data.humMax,"????????????", Color.BLUE);
            dynamicLineChartManager.setHightLimitLine(data.humMin,"????????????", Color.BLUE);
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