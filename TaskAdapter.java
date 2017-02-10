package personal.td7.com.mydaily;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Vector;

/**
 * Created by Tejas on 08-Feb-17.
 */

public class TaskAdapter extends ArrayAdapter<TaskSelector.Task> {
    Context c;
    int resource;
    Vector<TaskSelector.Task> data;

    TaskAdapter(Context c,int resource,Vector<TaskSelector.Task> data){
        super(c,resource,data);
        this.c = c;
        this.resource = resource;
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;

        PlaceHolder p;
        if(row == null){
            LayoutInflater inflater = LayoutInflater.from(c);
            row = inflater.inflate(resource,parent,false);

            TextView time = (TextView) row.findViewById(R.id.timeView);
            TextView task = (TextView) row.findViewById(R.id.taskView);
            ImageButton delete = (ImageButton) row.findViewById(R.id.deleteTask);

            p = new PlaceHolder();
            p.time = time;
            p.data = task;

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TaskSelector.deleteTask(data.get(position).id,data.get(position).name);
                    remove(data.get(position));
                }
            });

            row.setTag(p);
        }
        else{
            p = (PlaceHolder) row.getTag();
        }

        String time = data.get(position).time;
        p.time.setText(time);
        p.data.setText(data.get(position).name);
        p.id = data.get(position).id;

        return row;
    }

    private class PlaceHolder{
        TextView time;
        TextView data;
        ImageButton deleteTask;
        int id;
    }
}
