package com.neusoft.qiangzi.bluetoothservicedemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.os.Build.VERSION.SDK_INT;

public class BluetoothManager extends IBluetoothBinder.Stub {
    private static final String TAG = "BluetoothManager";
    public static final String SERVICE_NAME = "com.neusoft.qiangzi.bluetoothservicedemo.BluetoothService";
    public static final String SERVICE_PACKEG = "com.neusoft.qiangzi.bluetoothservicedemo";
    private Context context;
    private IBluetoothBinder binder;
    private IOnBluetoothListener listener;
    private OnBindedListener onBindedListener;
    private boolean isBinded = false;


    public BluetoothManager(Context context) {
        this.context = context;
    }

    public void start() {
        //如果服务没有启动，则启动服务
        if (!ServiceUtil.isServiceRunning(context, SERVICE_NAME)) {
            Intent i = new Intent();
            i.setComponent(new ComponentName(SERVICE_PACKEG, SERVICE_NAME));
            if (SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            }else {
                context.startService(i);
            }
        }
    }

    public void stop() {
        //如果服务没有启动，则启动服务
        if (ServiceUtil.isServiceRunning(context, SERVICE_NAME)) {
            Intent i = new Intent();
            i.setComponent(new ComponentName(SERVICE_PACKEG, SERVICE_NAME));
            context.stopService(i);
        }
    }

    public boolean isStared() {
        return ServiceUtil.isServiceRunning(context, SERVICE_NAME);
    }

    public void setOnBindedListener(OnBindedListener listener) {
        onBindedListener = listener;
    }

    public void setBluetoothListener(IOnBluetoothListener listener) {
        this.listener = listener;
    }
    public void bind() {
        if(isBinded)return;
        //绑定服务
        Intent i = new Intent();
        i.setComponent(new ComponentName(SERVICE_PACKEG, SERVICE_NAME));
        context.bindService(i, connection, BIND_AUTO_CREATE);
    }

    public void unbind() {
        if(!isBinded) return;
        if (binder != null) {
            try {
                binder.unregisterListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        //解绑服务
        if (context != null) {
            context.unbindService(connection);
        }
        isBinded = false;
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: is called");
            isBinded = true;
            binder = IBluetoothBinder.Stub.asInterface(iBinder);
            if (binder != null) {
                try {
                    binder.registerListener(listener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if(onBindedListener !=null) onBindedListener.onBinded();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: is called");
            binder = null;
            isBinded = false;
        }
    };

    public interface OnBindedListener {
        void onBinded();
    }

    ///////////以下为接口原有方法的实现//////////////
    @Override
    public boolean isConnected() {
        if(binder!=null) {
            try {
                return binder.isConnected();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void setBluetoothMode(int mode) {
        if(binder!=null) {
            try {
                binder.setBluetoothMode(mode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getBluetoothMode() {
        if(binder!=null) {
            try {
                return binder.getBluetoothMode();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public String getRemoteAddress() {
        if(binder!=null) {
            try {
                return binder.getRemoteAddress();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String getRemoteName() {
        if(binder!=null) {
            try {
                return binder.getRemoteName();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void setAutoConnectEnabled(boolean b) {
        if(binder!=null) {
            try {
                binder.setAutoConnectEnabled(b);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isAutoConnectEnabled() {
        if(binder!=null) {
            try {
                return binder.isAutoConnectEnabled();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void sendText(String text) {
        if(binder!=null) {
            try {
                binder.sendText(text);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendDate(String key, String value) {
        if(binder!=null) {
            try {
                binder.sendDate(key, value);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void connect(String address) {
        if(binder!=null) {
            try {
                binder.connect(address);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void disconnect() {
        if(binder!=null) {
            try {
                binder.disconnect();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void registerListener(IOnBluetoothListener listener) {
        if(binder!=null) {
            try {
                binder.registerListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void unregisterListener(IOnBluetoothListener listener) {
        if(binder!=null) {
            try {
                binder.unregisterListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
