package cn.edu.neusoft.lgx.stm32app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.MyViewHolder> {

    List<Datalist> Fldata;
    Context context;

    public DataAdapter(Context context,List<Datalist> fldata)
    {
        this.context = context;
        Fldata = fldata;
    }

    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_data,
                parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull  DataAdapter.MyViewHolder holder, int position) {
        holder.temShow.setText(String.valueOf(Fldata.get(position).tem));
        holder.humShow.setText(String.valueOf(Fldata.get(position).hum));
        holder.timeShow.setText(Fldata.get(position).time);
        int n = Fldata.get(position).worn;
        if(n != 0){
            holder.imageView.setImageResource(R.drawable.ic_baseline_warning_24);
            if (n/8 == 1)
                holder.temContorl.setText("降温");
            else if(n/4%2 == 1)
                holder.temContorl.setText("升温");
            if(n/2%2 == 1)
                holder.humControl.setText("除湿");
            else if(n%2 == 1)
                holder.humControl.setText("加湿");
        }
    }

    @Override
    public int getItemCount() {
        if(Fldata!=null)return Fldata.size();
        else return 0;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView temShow,temContorl,humShow,humControl,timeShow;
        ImageView imageView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            temShow = itemView.findViewById(R.id.textView_sqlit_temShow);
            temContorl = itemView.findViewById(R.id.textView_sqlit_temControl);
            humShow = itemView.findViewById(R.id.textView_sqlit_humShow);
            humControl = itemView.findViewById(R.id.textView_sqlit_humControl);
            timeShow = itemView.findViewById(R.id.textView_sqlit_timeShow);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
