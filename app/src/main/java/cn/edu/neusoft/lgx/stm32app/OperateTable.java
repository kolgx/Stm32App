package cn.edu.neusoft.lgx.stm32app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class OperateTable {
    private static final String table = "recode";
    private static final String table2 = "setlist";
    private SQLiteDatabase db = null;
    public OperateTable(SQLiteDatabase db){
        this.db = db;
    }

    public boolean insert(Datalist date){
        if(date == null)
            return false;
        if(date.tem != -1)
            return addDate(date.time,date.tem,date.hum,date.worn);
        else if(date.temMax != -1)
            return addSet(date.temMax,date.temMin,date.humMax,date.humMin);
        return false;
    }

    private boolean addDate(String time, int tem, int hum, int worn){
        ContentValues valuesfl = new ContentValues();
        valuesfl.put("time",time);
        valuesfl.put("tem",tem);
        valuesfl.put("hum",hum);
        valuesfl.put("worn",worn);
        long c = db.insert(table,null,valuesfl);
        this.db.close();
        if (c>0)return true;
        else return false;
    }

    private boolean addSet(int temMax, int temMin, int humMax, int humMin){
        ContentValues values = new ContentValues();
        values.put("temMax", temMax);
        values.put("temMin", temMin);
        values.put("humMax", humMax);
        values.put("humMin", humMin);
        long c = db.insert(table2,null,values);
        this.db.close();
        if (c>0)return true;
        else return false;
    }

    public void delete(){
        String sql = "DELETE FROM "+table;
        this.db.execSQL(sql);
        this.db.close();
    }
}
