package com.neusoft.qiangzi.bluetoothservicedemo;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

public class ServiceUtil {
    public static boolean isServiceRunning(Context context, String serviceName){
        // 校验服务是否还存在
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo info : services) {
            // 得到所有正在运行的服务的名称
            String name = info.service.getClassName();
            if (serviceName.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
