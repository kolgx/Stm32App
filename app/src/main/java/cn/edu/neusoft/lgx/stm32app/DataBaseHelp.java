package cn.edu.neusoft.lgx.stm32app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBaseHelp extends SQLiteOpenHelper {
    private static final String name = "selie.db";
    private static final String table = "recode";
    private static final String table2 = "setlist";
    public DataBaseHelp(Context context) {
        super(context, name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE if not exists "+table+" ("+
                "time DATETIME NOT NULL ,"+
                "tem INTEGER NOT NULL ,"+
                "hum INTEGER NOT NULL ,"+
                "worn INTEGER NOT NULL ,"+
                "check(worn = 0 or worn = 1 or worn = 2 or worn = 4 or worn = 8 or worn = 5 " +
                "or worn = 6 or worn = 9 or worn = 10)"+
                ")";
        db.execSQL(sql);
        String sql2 = "CREATE TABLE if not exists "+table2+" ("+
                "temMax INTEGER NOT NULL ,"+
                "temMin INTEGER NOT NULL ,"+
                "humMax INTEGER NOT NULL ,"+
                "humMin INTEGER NOT NULL ,"+
                "check(temMax > temMin and humMax > humMin))";
        db.execSQL(sql2);

        ContentValues values = new ContentValues();
        values.put("temMax", 99);
        values.put("temMin", 0);
        values.put("humMax", 99);
        values.put("humMin", 0);
        db.insert(table2,null,values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
