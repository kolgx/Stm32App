package cn.edu.neusoft.lgx.stm32app;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.neusoft.qiangzi.bluetoothservicedemo.BluetoothManager;
import com.neusoft.qiangzi.bluetoothservicedemo.IOnBluetoothListener;

public class SetActivity extends AppCompatActivity {

    private EditText temMax,temMin,humMax,humMin;
    private DataBaseHelp dataBaseHelp;
    private Datalist data;
    private BluetoothManager bluetoothManager;
    private final String TAG = "SetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        dataBaseHelp = new DataBaseHelp(this);
        SearchTable searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.setBluetoothListener(receivedListener);
        bluetoothManager.setOnBindedListener(new BluetoothManager.OnBindedListener() {
            @Override
            public void onBinded() {

            }
        });

        temMax = findViewById(R.id.editTextNumber_temMax);
        temMin = findViewById(R.id.editTextNumber_temMin);
        humMax = findViewById(R.id.editTextNumber_humMax);
        humMin = findViewById(R.id.editTextNumber_humMin);

         data = searchTable.getSet();
        if(data != null){
            temMax.setText(String.valueOf(data.temMax));
            temMin.setText(String.valueOf(data.temMin));
            humMax.setText(String.valueOf(data.humMax));
            humMin.setText(String.valueOf(data.humMin));
        }
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

    private Runnable sendData = new Runnable() {
        Handler delay = new Handler();
        @Override
        public void run() {
            try {
                bluetoothManager.sendText(" ");
            }catch (Exception e){
                Toast.makeText(SetActivity.this,"发送数据头失败",Toast.LENGTH_SHORT).show();
            }
            delay.postDelayed(this::sendSecond,500);
        }
        public void sendSecond(){
            try {
                bluetoothManager.sendText(String.valueOf(data.temMax*100+data.temMin)+
                        (data.humMax * 100 + data.humMin) +" ");
            }catch (Exception e){
                Toast.makeText(SetActivity.this,"发送数据体失败",Toast.LENGTH_SHORT).show();
            }
            delay.postDelayed(this::sendChack,500);
        }
        public void sendChack(){
            if(!isSendSuccess)
                Toast.makeText(SetActivity.this,"数据同步失败，请重试！",Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(SetActivity.this,"数据同步成功！",Toast.LENGTH_SHORT).show();
        }
    };

    private boolean isSendSuccess = false;
    //创建服务事件监听器对象，用于接收服务的主动通知
    IOnBluetoothListener receivedListener = new IOnBluetoothListener.Stub() {
        //蓝牙数据通过这个函数传给activity
        @Override
        public void onReceived(String key, String value) throws RemoteException {
            if(key.equals("usartGet"))
                isSendSuccess = true;
        }
        @Override
        public void onEvent(int e) throws RemoteException {
        }
    };

    public void saveButtonClick(View view){
        int [] n = new int[4];
        n[0] = Integer.parseInt(temMax.getText().toString());
        n[1] = Integer.parseInt(temMin.getText().toString());
        n[2] = Integer.parseInt(humMax.getText().toString());
        n[3] = Integer.parseInt(humMin.getText().toString());
        if(n[0] != data.temMax && n[0] >= 0 && n[0] < 100)
            data.temMax = n[0];
        if(n[1] != data.temMin && n[1] >= 0 && n[1] < 100)
            data.temMin = n[1];
        if(n[2] != data.humMax && n[2] >= 0 && n[2] < 100)
            data.humMax = n[2];
        if(n[3] != data.humMin && n[3] >= 0 && n[3] < 100)
            data.humMin = n[3];
        OperateTable operateTable = new OperateTable(dataBaseHelp.getWritableDatabase());
        if( ! operateTable.insert(data))
            Toast.makeText(this,"设置保存失败!",Toast.LENGTH_LONG).show();
        sendData.run();
    }

    public void quiteButtonClick(View view){
        finish();
    }

}