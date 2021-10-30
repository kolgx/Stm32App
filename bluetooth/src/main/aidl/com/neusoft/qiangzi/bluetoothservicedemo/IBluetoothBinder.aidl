// IBluetoothBinder.aidl
package com.neusoft.qiangzi.bluetoothservicedemo;
import com.neusoft.qiangzi.bluetoothservicedemo.IOnBluetoothListener;

// Declare any non-default types here with import statements

interface IBluetoothBinder {
    const String SERVICE_NAME = "com.neusoft.qiangzi.bluetoothservicedemo.BluetoothService";
    const int MODE_SERVER = 0;
    const int MODE_CLIENT = 1;
    //数据类型定义
    const String DATA_TYPE_ENGINE = "CAR_ENGINE";
    const String DATA_TYPE_SPEED = "CAR_SPEED";
    const String DATA_TYPE_BATTARY = "BATTARY";
    const String DATA_TYPE_ODOMETER = "ODOMETER";
    //定义广播action
    const String ACTION_ENGINE_STATE_CHANGED = "com.neusoft.qiangzi.bluetoothservicedemo.BluetoothService.EngineState";

    boolean isConnected();

    void setBluetoothMode(int mode);
    int getBluetoothMode();
    String getRemoteAddress();
    String getRemoteName();
    void setAutoConnectEnabled(boolean b);
    boolean isAutoConnectEnabled();

    void sendText(String text);
    void sendDate(String key, String value);

    void connect(String address);
    void disconnect();

    void registerListener(IOnBluetoothListener listener);
    void unregisterListener(IOnBluetoothListener listener);
}