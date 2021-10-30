// IOnBluetoothListener.aidl
package com.neusoft.qiangzi.bluetoothservicedemo;

// Declare any non-default types here with import statements

interface IOnBluetoothListener {
    const int CONNECT_SUCCESS = 0;
    const int DISCONNECT_SUCCESS = 1;
    const int ACCEPT_SUCCESS = 2;
    const int CONNECT_FAILED = -1;
    const int BREAK_OFF = -3;
    const int SEND_ERROR = -4;
    const int RECV_ERROR = -5;
    const int UNKNOWN_ERROR = -7;

    void onReceived(String key, String value);
    void onEvent(int e);
}