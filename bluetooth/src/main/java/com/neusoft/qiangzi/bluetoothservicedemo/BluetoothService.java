package com.neusoft.qiangzi.bluetoothservicedemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import androidx.core.app.NotificationCompat;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final String BT_UUID = "00001101-0000-1000-8000-00805F9B34FB";
//    private static final String BT_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66";
    private static final int WHAT_BT_CONNECT_OK = 1;
    private static final int WHAR_BT_DISCONNECT_OK = 6;
    private static final int WHAT_BT_CONNECT_NG = 2;
    private static final int WHAT_BT_ACCEPT_OK = 3;
    private static final int WHAT_BT_RECV_DATA = 4;
    private static final int WHAT_BT_RECV_INTERUPT = 5;

    public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";
    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";
    public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";
    public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

    private Thread recvThread;
    private Thread listenThread;
    private Thread autoConnectThread;
    private BluetoothAdapter mBluetoothAdapter;//蓝牙是入口
    private BluetoothDevice mBluetoothDevice;//将要连接的指定设备
    private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothSocket btSocket;
    private BluetoothServerSocket btServerSocket;
    private int bluetoothMode;
    private String remoteAddress;
    private String remoteDeviceName;
    private boolean isAutoConnect;
    private boolean isServiceExit;
    private RemoteCallbackList<IOnBluetoothListener> mListenerList = new RemoteCallbackList<>();

    public BluetoothService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceExit = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences shp = getSharedPreferences("bluetooth_config", MODE_PRIVATE);
        bluetoothMode = shp.getInt("bluetooth_mode", IBluetoothBinder.MODE_CLIENT);
        remoteAddress = shp.getString("remote_device_address", null);
        remoteDeviceName = shp.getString("remote_device_name", null);
        isAutoConnect = shp.getBoolean("is_auto_connect", false);
        if(isAutoConnect) startAutoConnectThread();
        if(bluetoothMode==IBluetoothBinder.MODE_SERVER)startListen();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: is called");
        NotificationChannel notificationChannel = null;
        String CHANNEL_ID = getClass().getPackage().toString();
        String CHANNEL_NAME = "Bluetooth Servier";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification notification = new NotificationCompat.Builder(getApplicationContext(),CHANNEL_ID)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("蓝牙服务")
                .setContentText("蓝牙服务运行中。。。")
                .setWhen(System.currentTimeMillis())
                .build();
        startForeground(1,notification);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: is called.");
        return new IBluetoothBinder.Stub() {
            @Override
            public boolean isConnected() throws RemoteException {
                if(btSocket !=null)
                    return btSocket.isConnected();
                return false;
            }

            @Override
            public void setBluetoothMode(int mode) throws RemoteException {
                if (bluetoothMode == mode) {
                    Log.w(TAG, "setBluetoothMode: mode no change, do nothing.");
                    return;
                }
                bluetoothMode = mode;
                switch (mode) {
                    case IBluetoothBinder.MODE_SERVER:
                        Log.d(TAG, "setBluetoothMode: server");
                        //断开现有连接，启动监听
                        BluetoothService.this.disconnect();
                        startListen();
                        //启动修改蓝牙可见性的Intent
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        //设置蓝牙可见性的时间，方法本身规定最多可见300秒
                        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                    case IBluetoothBinder.MODE_CLIENT:
                        Log.d(TAG, "setBluetoothMode: client");
                        BluetoothService.this.disconnect();
                        stopListen();
                        if(isAutoConnect) autoConnectThreadNotify();
                        break;
                }
                //保存工作模式
                SharedPreferences shp = getSharedPreferences("bluetooth_config", MODE_PRIVATE);
                SharedPreferences.Editor editor = shp.edit();
                editor.putInt("bluetooth_mode", mode);
                editor.commit();
            }

            @Override
            public int getBluetoothMode() throws RemoteException {
                return bluetoothMode;
            }

            @Override
            public String getRemoteAddress() throws RemoteException {
                return remoteAddress;
            }

            @Override
            public String getRemoteName() throws RemoteException {
                return remoteDeviceName;
            }

            @Override
            public void setAutoConnectEnabled(boolean b) throws RemoteException {
                if (isAutoConnect == b) {
                    Log.w(TAG, "setAutoConnectEnabled: no change, do nothing.");
                    return;
                }
                isAutoConnect = b;
                if (b) {
                    startAutoConnectThread();
                } else {
                    autoConnectThreadNotify();
                }
                //保存自动连接
                SharedPreferences shp = getSharedPreferences("bluetooth_config", MODE_PRIVATE);
                SharedPreferences.Editor editor = shp.edit();
                editor.putBoolean("is_auto_connect", isAutoConnect);
                editor.commit();
            }

            @Override
            public boolean isAutoConnectEnabled() throws RemoteException {
                return isAutoConnect;
            }

            @Override
            public void sendText(String text) throws RemoteException {
                send(text);
            }

            @Override
            public void sendDate(String key, String value) throws RemoteException {
                if(key==null || key.isEmpty()) key = "DATA";
                sendText(key+"="+value);
            }

            @Override
            public void connect(String address) throws RemoteException {
                if(bluetoothMode==MODE_CLIENT)
                    BluetoothService.this.connect(address);
            }

            @Override
            public void disconnect() throws RemoteException {
                BluetoothService.this.disconnect();
            }

            @Override
            public void registerListener(IOnBluetoothListener listener) throws RemoteException {
                mListenerList.register(listener);
                Log.d(TAG, "registerListener: current size:" + mListenerList.getRegisteredCallbackCount());
            }

            @Override
            public void unregisterListener(IOnBluetoothListener listener) throws RemoteException {
                mListenerList.unregister(listener);
                Log.d(TAG, "unregisterListener: current size:" + mListenerList.getRegisteredCallbackCount());
            }
        };
    }

    private void broadcastReceivedData(String data) {
        synchronized (mListenerList) {
            int n = mListenerList.beginBroadcast();
//            Log.d(TAG, "broadcastReceivedData: begin n="+n);
            try {
                //解析数据
                String[] strs = data.split("=");
                String key,value;
                if (strs.length == 2) {
                    key = strs[0];
                    value = strs[1];
                }else {
                    key = "DATA";
                    value = data;
                }
                //回调函数
                for (int i = 0; i < n; i++) {
                    IOnBluetoothListener listener = mListenerList.getBroadcastItem(i);
                    if (listener != null) {
                        //通知监听段
                        listener.onReceived(key,value);
                    }
                }
                //发送广播
                if (strs[0].equals(IBluetoothBinder.DATA_TYPE_ENGINE)) {
                    Log.d(TAG, "broadcastReceivedData: send broadcast: DATA_TYPE_ENGINE");
                    Intent i = new Intent(IBluetoothBinder.ACTION_ENGINE_STATE_CHANGED);
                    i.putExtra("state",strs[1]);
                    sendBroadcast(i);
                }

            } catch (RemoteException e) {
                Log.e(TAG, "broadcastReceivedData: error!");
                e.printStackTrace();
            }
            mListenerList.finishBroadcast();
//            Log.d(TAG, "broadcastReceivedData: end");
        }
    }
    private void broadcastEvent(int event) {
        synchronized (mListenerList) {
            int n = mListenerList.beginBroadcast();
//            Log.d(TAG, "broadcastEvent: begin n="+n);
            try {
                for (int i = 0; i < n; i++) {
                    IOnBluetoothListener listener = mListenerList.getBroadcastItem(i);
                    if (listener != null) {
                        listener.onEvent(event);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "broadcastEvent: error!");
                e.printStackTrace();
            }
            mListenerList.finishBroadcast();
//            Log.d(TAG, "broadcastEvent: end");
        }
    }

    private void startListen() {
        if (listenThread != null && listenThread.isAlive()) {
            Log.e(TAG, "listen: server thread is still started.");
            return;
        }
        listenThread = new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "listenThread: start");
                try {
                    btServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                            PROTOCOL_SCHEME_RFCOMM, UUID.fromString(BT_UUID)
                    );
                    while (!isServiceExit) {
                        Log.d(TAG, "listenThread: is waiting connect...");
                        btSocket = btServerSocket.accept();
                        Log.d(TAG, "run: bluetooth accepted");
                        remoteAddress = btSocket.getRemoteDevice().getAddress();
                        remoteDeviceName = btSocket.getRemoteDevice().getName();
                        //获取输出流
                        outputStream = btSocket.getOutputStream();
                        inputStream = btSocket.getInputStream();
                        startRecvThread();
                        handler.sendEmptyMessage(WHAT_BT_ACCEPT_OK);
                        Log.d(TAG, "accept run: bluetooth connect success!");
                    }
                } catch (EOFException e) {
                    Log.e(TAG, "run: client has closed");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.w(TAG, "run: server socket error. server socket may be closed.");
//                    e.printStackTrace();
                }
                Log.d(TAG, "listenThread: end");
            }
        };
        listenThread.start();
    }
    private void connect(String address){
        if (address!=null&&!address.isEmpty()) {
            Log.d(TAG, "connect: address="+address);
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
            remoteAddress = address;
        }
        else {
            mBluetoothDevice = null;
            Log.e(TAG, "connect: address error.");
            return;
        }

        if (mBluetoothDevice != null) {//如果发现了指定的设备
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (btSocket != null) {
                            btSocket.close();
                        }
                        btSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(BT_UUID));
                        //开始交换数据
                        btSocket.connect();//连接
                        remoteDeviceName = mBluetoothDevice.getName();
                        //获取输出流
                        outputStream = btSocket.getOutputStream();
                        inputStream = btSocket.getInputStream();
                        startRecvThread();
//                        Toast.makeText(getBaseContext(),"蓝牙连接成功！",Toast.LENGTH_SHORT).show();
                        handler.sendEmptyMessage(WHAT_BT_CONNECT_OK);
                        //保存地址
                        SharedPreferences shp = getSharedPreferences("bluetooth_config", MODE_PRIVATE);
                        SharedPreferences.Editor editor = shp.edit();
                        editor.putString("remote_device_address", remoteAddress);
                        editor.putString("remote_device_name", remoteDeviceName);
                        editor.commit();
                        Log.d(TAG, "connect run: bluetooth connect success!");
                    } catch (IOException e) {
                        Log.e(TAG, "connectThread: connect failed.");
                        handler.sendEmptyMessage(WHAT_BT_CONNECT_NG);
//                        e.printStackTrace();
                    }
                }
            }.start();
        } else {
//            Toast.makeText(getBaseContext(),"蓝牙连接失败！",Toast.LENGTH_SHORT).show();
            Log.e(TAG, "connect: failed. mBluetoothDevice=null. address="+address);
            handler.sendEmptyMessage(WHAT_BT_CONNECT_NG);
        }
    }

    private void stopListen() {
        if (btServerSocket != null) {
            try {
                btServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            btServerSocket = null;
        }
    }
    private void disconnect(){
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            btSocket = null;
        }
    }
    private void send(String str) {
        if(str.isEmpty())return;
        try {
            if (outputStream != null) {
                outputStream.write(str.getBytes());//写入数据
                outputStream.flush();
                Log.d(TAG,"BT Send="+ str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case WHAT_BT_CONNECT_OK:
                    broadcastEvent(IOnBluetoothListener.CONNECT_SUCCESS);
                    break;
                case WHAR_BT_DISCONNECT_OK:
                    broadcastEvent(IOnBluetoothListener.DISCONNECT_SUCCESS);
                    break;
                case WHAT_BT_ACCEPT_OK:
                    broadcastEvent(IOnBluetoothListener.ACCEPT_SUCCESS);
                    break;
                case WHAT_BT_CONNECT_NG:
                    broadcastEvent(IOnBluetoothListener.CONNECT_FAILED);
                    break;
                case WHAT_BT_RECV_DATA:
                    broadcastReceivedData((String)msg.obj);
                    break;
                case WHAT_BT_RECV_INTERUPT:
                    broadcastEvent(IOnBluetoothListener.BREAK_OFF);
                    break;
            }
        }
    };

    private void startRecvThread() {
        if(recvThread!=null && recvThread.isAlive()){
            Log.e(TAG, "startRecvThread: thread is still started.");
            return;
        }
        recvThread = new Thread(){
            @Override
            public void run() {
                Log.d(TAG, "run: recvThread start.");
                //等待接收蓝牙数据
                while (!isServiceExit) {
                    if (btSocket !=null
                            && btSocket.isConnected()
                            && inputStream != null) {
                        try {
                            byte[] data = new byte[100];
//                                inputStream.wait(1000);
                            int len = inputStream.read(data);
                            Message msg = handler.obtainMessage(WHAT_BT_RECV_DATA);
                            msg.obj = new String(data,0,len);
                            handler.sendMessage(msg);
                        } catch (IOException e) {
                            Log.w(TAG, "bluetooth read error! maybe bt is disconnected.");
                            if(btSocket!=null&&btSocket.isConnected()) {
                                try {
                                    btSocket.close();
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                                btSocket = null;
                                handler.sendEmptyMessage(WHAT_BT_RECV_INTERUPT);
                            }
                            else handler.sendEmptyMessage(WHAR_BT_DISCONNECT_OK);
                            if(isAutoConnect)autoConnectThreadNotify();
                            recvThread = null;
                            return;
                        }
                    } else {
                        Log.d(TAG, "run: bt is not connected. waiting...");
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        recvThread.start();
    }

    private Object autoConnectThreadLock = new Object();
    private boolean isAutoConnectThreadWait = false;
    private void startAutoConnectThread() {
        if(autoConnectThread!=null && autoConnectThread.isAlive()){
            Log.w(TAG, "startAutoConnectThread: is still running");
            return;
        }
        autoConnectThread = new Thread(){
            @Override
            public void run() {
                super.run();
                int time = 10000;
                int counter = 0;
                Log.d(TAG, "autoConnectThread: start");
                synchronized (autoConnectThreadLock) {
                    while (isAutoConnect && !isServiceExit) {
                        if (bluetoothMode == IBluetoothBinder.MODE_CLIENT
                                && (btSocket == null || btSocket != null && !btSocket.isConnected())
                                && remoteAddress != null && !remoteAddress.isEmpty()) {
                            Log.d(TAG, "autoConnectThread: try to connect...");
                            connect(remoteAddress);
                        }
                        //工作在服务模式下，进入这个地方等待，直到被模式切换唤醒
                        if (bluetoothMode == IBluetoothBinder.MODE_SERVER) {
                            Log.d(TAG, "autoConnectThread: server mode");
                            autoConnectThreadWait(0);
                            time = 10000;
                            counter = 0;
                        }
                        //工作在客户端模式下，并已经连接成功，进入这里等待，直到被通信中断唤醒
                        else if (btSocket != null && btSocket.isConnected()) {
                            Log.d(TAG, "autoConnectThread: client mode connected");
                            autoConnectThreadWait(0);
                            time = 10000;
                            counter = 0;
                        }
                        //未连接的情况下进入这里，等待一段时间之后重新连接，会被模式切换唤醒
                        else {
                            counter++;
                            time = Math.min(time+counter*1000, 60000);
                            autoConnectThreadWait(time);
                        }

                    }
                }
                Log.d(TAG, "autoConnectThread: end");
            }
        };
        autoConnectThread.start();
    }

    //自动连接线程等待休眠的函数，这个等待可以被其他线程唤醒
    private void autoConnectThreadWait(int time) {
        try {
            isAutoConnectThreadWait = true;
            if(time<=0) {
                Log.d(TAG, "autoConnectThread: go to wait...");
                autoConnectThreadLock.wait();
            }
            else {
                Log.d(TAG, "autoConnectThread: go to wait("+time/1000+"s)...");
                autoConnectThreadLock.wait(time);
            }
            Log.d(TAG, "autoConnectThread: wakeup.");
        } catch (InterruptedException e) {
            Log.e(TAG, "autoConnectThread: wait error.");
            e.printStackTrace();
        }
        isAutoConnectThreadWait = false;
    }
    //唤醒上面等待函数
    private void autoConnectThreadNotify() {
        if(isAutoConnectThreadWait)
            synchronized (autoConnectThreadLock){
                Log.d(TAG, "setBluetoothMode: notify to wakeup autoConnectThread");
                autoConnectThreadLock.notify();
            }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceExit = true;
        disconnect();
        stopListen();
    }

}