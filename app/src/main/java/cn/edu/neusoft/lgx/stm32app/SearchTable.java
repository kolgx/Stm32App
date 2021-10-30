package cn.edu.neusoft.lgx.stm32app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class SearchTable {
    private static final String table = "recode";
    private static final String table2 = "setlist";
    private SQLiteDatabase db = null;

    public SearchTable(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * 获取所有记录
     * @return 所有记录，注意：需要判断是否为空
     */
    public List<Datalist> getall() {
        List<Datalist> data = new ArrayList<>();
        Cursor c = db.query(table,null,null,
                null,null,null,null);
        while (c.moveToNext())
        {
            Datalist fl = new Datalist();
            fl.time = c.getString(c.getColumnIndex("time"));
            fl.tem = c.getInt(c.getColumnIndex("tem"));
            fl.hum = c.getInt(c.getColumnIndex("hum"));
            fl.worn = c.getInt(c.getColumnIndex("worn"));
            data.add(fl);
        }
        this.db.close();
        return data;
    }

    public Datalist getLast(){
        Datalist fl = new Datalist();
        Cursor c = db.query(table,null,null,
                null,null,null,null);
        if(c.isAfterLast()){
            fl.time = "00:00:00";
            fl.tem = 20;
            fl.hum = 20;
            fl.worn = 0;
        }
        else {
            c.moveToNext();
            fl.time = c.getString(c.getColumnIndex("time"));
            fl.tem = c.getInt(c.getColumnIndex("tem"));
            fl.hum = c.getInt(c.getColumnIndex("hum"));
            fl.worn = c.getInt(c.getColumnIndex("temControl"));
        }
        this.db.close();
        return fl;
    }

    /**
     * 筛选记录
     * @return 筛选记录，注意：需要判断是否为空
     */
    public List<Datalist> getrecode(int n) {
        List<Datalist> data = new ArrayList<>();
        String[] selectionArgs = new  String[]{String.valueOf(n)};
        Cursor c = db.query(table,null,"worn=?",
                selectionArgs,null,null,null);
        while (c.moveToNext())
        {
            Datalist fl = new Datalist();
            fl.time = c.getString(c.getColumnIndex("time"));
            fl.tem = c.getInt(c.getColumnIndex("tem"));
            fl.hum = c.getInt(c.getColumnIndex("hum"));
            fl.worn = c.getInt(c.getColumnIndex("worn"));
            data.add(fl);
        }
        this.db.close();
        return data;
    }

    public Datalist getSet(){
        Datalist s1 = new Datalist();
        return getSet(s1);
    }

    public Datalist getSet(Datalist s1){
        Cursor c = db.query(table2,null,null,
                null,null,null,null);
        if(c.isAfterLast()){
            s1.temMax = 99;
            s1.temMin = 0;
            s1.humMax = 99;
            s1.humMin = 0;
        }else {
            c.moveToLast();
            s1.temMax = c.getInt(c.getColumnIndex("temMax"));
            s1.temMin = c.getInt(c.getColumnIndex("temMin"));
            s1.humMax = c.getInt(c.getColumnIndex("humMax"));
            s1.humMin = c.getInt(c.getColumnIndex("humMin"));
        }
        this.db.close();
        return s1;
    }
}
