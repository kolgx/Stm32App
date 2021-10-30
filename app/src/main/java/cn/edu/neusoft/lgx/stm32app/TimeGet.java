package cn.edu.neusoft.lgx.stm32app;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeGet {
    public static String GetTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        //获取当前时间
        Calendar calendar = Calendar.getInstance();
        Date date1 = calendar.getTime();
        String fi_indate = sdf.format(date1);
        return fi_indate;
    }
    public static String GetDay(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        //获取当前时间
        Calendar calendar = Calendar.getInstance();
        Date date1 = calendar.getTime();
        String fi_indate = sdf.format(date1);
        return fi_indate;
    }
}
