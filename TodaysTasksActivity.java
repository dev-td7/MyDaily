package personal.td7.com.mydaily;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.Date;
import java.util.Vector;

/**
 * Created by Tejas on 18-Feb-17.
 */

public class TodaysTasksActivity extends AppCompatActivity {

    static Date dt = TaskSelector.dt;
    static Vector<TaskSelector.Task> data;
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasks_today);
        //Manage this later
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dt = TaskSelector.dt;
        data = TaskSelector.v;

        TaskAdapter t = new TaskAdapter(getApplicationContext(),R.layout.list_content,data);
        ListView today = (ListView) findViewById(R.id.today);
        today.setAdapter(t);

        //Set add task to fab
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        NavUtils.navigateUpTo(this,new Intent(getApplicationContext(),TaskSelector.class));
        return super.onOptionsItemSelected(item);
    }
}
