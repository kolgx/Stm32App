package com.neusoft.qiangzi.bluetoothservicedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, Switch.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener {
    private static final String TAG = "MainActivity";

    private TextView tvBluetoothState;
    private RadioGroup rgBluetoothMode;
    private RadioButton rbSlaveMode;
    private RadioButton rbMasterMode;
    private Switch swBluetoothService;
    private Switch swAircontrol;
    private Button btConnect;
    private Button btDisconnect;
    private ToggleButton tbFire;
    private TextView tvRecvData;
    private CheckBox cbAutoConnect;

    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //关联UI控件
        tvBluetoothState = findViewById(R.id.tv_connection_state);
        rgBluetoothMode = findViewById(R.id.radioGroup_btmode);
        rbSlaveMode = findViewById(R.id.radioButton_slave);
        rbMasterMode = findViewById(R.id.radioButton_master);
        swBluetoothService = findViewById(R.id.switch_btservice);
        swAircontrol = findViewById(R.id.switch_aircontrol);
        btConnect = findViewById(R.id.button_connect);
        btDisconnect = findViewById(R.id.button_disconnect);
        tbFire = findViewById(R.id.toggleButton_fire);
        tvRecvData = findViewById(R.id.textView_recvdata);
        cbAutoConnect = findViewById(R.id.checkBox_autoconnect);

        //设置点击事件
        btConnect.setOnClickListener(this);
        btDisconnect.setOnClickListener(this);
        swBluetoothService.setOnCheckedChangeListener(this);
        swAircontrol.setOnCheckedChangeListener(this);
        rgBluetoothMode.setOnCheckedChangeListener(this);
        tbFire.setOnCheckedChangeListener(this);
        cbAutoConnect.setOnCheckedChangeListener(this);
        findViewById(R.id.button_clear).setOnClickListener(this);

        setViewEnabled(false);
        tvBluetoothState.setText("蓝牙未连接");

        //创建服务管理器对象
        bluetoothManager = new BluetoothManager(this);
        bluetoothManager.setBluetoothListener(receivedListener);
        bluetoothManager.setOnBindedListener(new BluetoothManager.OnBindedListener() {
            @Override
            public void onBinded() {
                Log.d(TAG, "onBinded: bind service.");
                //恢复状态
                switch (bluetoothManager.getBluetoothMode()) {
                    case IBluetoothBinder.MODE_CLIENT:
                        rbSlaveMode.setChecked(true);
                        break;
                    case IBluetoothBinder.MODE_SERVER:
                        rbMasterMode.setChecked(true);
                        break;
                }
                if (bluetoothManager.isConnected()) {
                    tvBluetoothState.setText("蓝牙已连接：" + bluetoothManager.getRemoteName());
                } else {
                    tvBluetoothState.setText("蓝牙未连接");
                }
                cbAutoConnect.setChecked(bluetoothManager.isAutoConnectEnabled());
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.button_connect) {
            Intent i = new Intent(MainActivity.this, BluetoothList.class);
            startActivityForResult(i, 1);
        } else if (id == R.id.button_disconnect) {
            bluetoothManager.disconnect();
        } else if (id == R.id.button_clear) {
            tvRecvData.setText("");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(!compoundButton.isPressed())return;

        Log.d(TAG, "onCheckedChanged: is called by " + compoundButton.getText());
        int id = compoundButton.getId();
        if (id == R.id.switch_btservice) {
            setViewEnabled(b);
            if (b) {
                bluetoothManager.start();
                bluetoothManager.bind();
            } else {
                bluetoothManager.unbind();
                bluetoothManager.stop();
            }
        } else if (id == R.id.switch_aircontrol) {
            bluetoothManager.sendDate(IBluetoothBinder.DATA_TYPE_SPEED, "1");
        } else if (id == R.id.toggleButton_fire) {
            if (b) bluetoothManager.sendDate(IBluetoothBinder.DATA_TYPE_ENGINE, "no");
            else bluetoothManager.sendDate(IBluetoothBinder.DATA_TYPE_ENGINE, "off");
        } else if (id == R.id.checkBox_autoconnect) {
            bluetoothManager.setAutoConnectEnabled(b);
        }
    }

    //没开启服务的时候，其他控件不可用
    private void setViewEnabled(boolean b) {
        swBluetoothService.setChecked(b);
        rbSlaveMode.setEnabled(b);
        rbMasterMode.setEnabled(b);
        btConnect.setEnabled(b);
        btDisconnect.setEnabled(b);
        tbFire.setEnabled(b);
        swAircontrol.setEnabled(b);
        cbAutoConnect.setEnabled(b);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        if(!rbSlaveMode.isPressed() && !rbMasterMode.isPressed())return;
        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.radioButton_slave) {
            bluetoothManager.setBluetoothMode(IBluetoothBinder.MODE_CLIENT);
        } else if (checkedRadioButtonId == R.id.radioButton_master) {
            bluetoothManager.setBluetoothMode(IBluetoothBinder.MODE_SERVER);
        }

    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: received broadcast");
            if (intent.getAction().equals(IBluetoothBinder.ACTION_ENGINE_STATE_CHANGED)) {
                Log.d(TAG, "onReceive: ACTION_ENGINE_STATE_CHANGED");
                if (intent.getStringExtra("state").equals("no"))
                    Toast.makeText(MainActivity.this, "收到广播：汽车点火！", Toast.LENGTH_LONG).show();
                else if (intent.getStringExtra("state").equals("off"))
                    Toast.makeText(MainActivity.this, "收到广播：汽车熄火！", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        //判断服务是否开启了，如果开启了则绑定服务
        if (bluetoothManager.isStared()) {
            bluetoothManager.bind();
        }
        setViewEnabled(bluetoothManager.isStared());

        //注册广播接收器
        IntentFilter filter = new IntentFilter(IBluetoothBinder.ACTION_ENGINE_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //如果绑定了服务，则要解绑服务
        bluetoothManager.unbind();
        //注销广播接收器
        unregisterReceiver(receiver);
    }

    //创建服务事件监听器对象，用于接收服务的主动通知
    IOnBluetoothListener receivedListener = new IOnBluetoothListener.Stub() {
        //蓝牙数据通过这个函数传给activity
        @Override
        public void onReceived(String key, String value) throws RemoteException {
            tvRecvData.append(key + "=" + value + " ");
        }

        //蓝牙的事件通过这个函数通知activity
        @Override
        public void onEvent(int e) throws RemoteException {
            switch (e) {
                case IOnBluetoothListener.CONNECT_SUCCESS:
                    Toast.makeText(MainActivity.this, "蓝牙连接成功！", Toast.LENGTH_SHORT).show();
                    tvBluetoothState.setText("已连接:" + bluetoothManager.getRemoteName());
                    break;
                case IOnBluetoothListener.ACCEPT_SUCCESS:
                    Toast.makeText(MainActivity.this, "蓝牙客户端连接成功！", Toast.LENGTH_SHORT).show();
                    tvBluetoothState.setText("已连接:" + bluetoothManager.getRemoteName());
                    break;
                case IOnBluetoothListener.CONNECT_FAILED:
                    Toast.makeText(MainActivity.this, "蓝牙连接失败！", Toast.LENGTH_SHORT).show();
                    break;
                case IOnBluetoothListener.DISCONNECT_SUCCESS:
                    Toast.makeText(MainActivity.this, "蓝牙断开成功！", Toast.LENGTH_SHORT).show();
                    tvBluetoothState.setText("蓝牙未连接");
                    break;
                case IOnBluetoothListener.BREAK_OFF:
                    Toast.makeText(MainActivity.this, "蓝牙通信中断！", Toast.LENGTH_SHORT).show();
                    tvBluetoothState.setText("蓝牙未连接");
                    break;
            }
        }
    };

    //连接蓝牙的时候，蓝牙设备搜索列表通过这个函数返回你所选择的蓝牙设备
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (data == null) return;
                String address = data.getStringExtra("address");
                bluetoothManager.connect(address);
                break;
        }
    }


}