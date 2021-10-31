package cn.edu.neusoft.lgx.stm32app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataActivity extends AppCompatActivity {

    private DataAdapter dataAdapter;
    private DataBaseHelp dataBaseHelp;
    private SearchTable searchTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        dataBaseHelp = new DataBaseHelp(this);
        SearchTable searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());

        List<Datalist> datalist = searchTable.getall();
        dataAdapter = new DataAdapter(this,datalist);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(dataAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sqlit_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.allShow:
                searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
                dataAdapter.Fldata = searchTable.getall();
                dataAdapter.notifyDataSetChanged();
                break;
            case R.id.temHight:
                searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
                dataAdapter.Fldata = searchTable.getrecode(Arrays.asList(8,9,10));
                dataAdapter.notifyDataSetChanged();
                break;
            case R.id.temLow:
                searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
                dataAdapter.Fldata = searchTable.getrecode(Arrays.asList(4,5,6));
                dataAdapter.notifyDataSetChanged();
                break;
            case R.id.humHight:
                searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
                dataAdapter.Fldata = searchTable.getrecode(Arrays.asList(2,6,10));
                dataAdapter.notifyDataSetChanged();
                break;
            case R.id.humLow:
                searchTable = new SearchTable(dataBaseHelp.getReadableDatabase());
                dataAdapter.Fldata = searchTable.getrecode(Arrays.asList(1,5,9));
                dataAdapter.notifyDataSetChanged();
                break;
            case R.id.allClear:
                OperateTable operateTable = new OperateTable(dataBaseHelp.getWritableDatabase());
                operateTable.delete();
                dataAdapter.Fldata = new ArrayList<>();
                dataAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}