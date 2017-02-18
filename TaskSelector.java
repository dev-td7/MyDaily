package personal.td7.com.mydaily;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

public class TaskSelector extends AppCompatActivity {

    CalendarView c;
    ListView taskview;
    static Date dt;
    String time, name;
    Dialog d;
    int nextId = 0;
    static SQLiteDatabase db;
    static final String EVENT_DESCRIPTION = "Added from My Daily App";
    static final int CALENDAR_ID = 1;    //Default Calendar
    static BackgroundHandler deleter;
    static boolean hasPermission = false;
    static final String DEFAULT_EVENT_NAME = "My Task";
    static Vector<Task> v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selector);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        dt = new Date();
        String yr = dt.getYear()+"";
        yr = "20" + yr.substring(1);
        dt.setYear(Integer.parseInt(yr));

        db = openOrCreateDatabase("Tasks", MODE_PRIVATE, null);
        deleter = new BackgroundHandler();

        new BackgroundHandler().execute(-1);
        new BackgroundHandler().execute(0,dt.getDate(),dt.getMonth(),dt.getYear());

        Log.d("Start nextID",nextId+"");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED){
                hasPermission = false;
            }
            else hasPermission = true;
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                d = new Dialog(TaskSelector.this);
                d.setContentView(R.layout.addtask);
                Drawable dr = new ColorDrawable(Color.BLACK);
                dr.setAlpha(60);
                d.getWindow().setBackgroundDrawable(dr);
                d.show();
                time = "All Day";

                final EditText taskName = (EditText) d.findViewById(R.id.taskName);
                Button ok = (Button) d.findViewById(R.id.btnOk);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        name = taskName.getText().toString();
                        new BackgroundHandler().execute(1, name, time);
                        new BackgroundHandler().execute(0, dt.getDate(), dt.getMonth(), dt.getYear());
                        d.hide();
                    }
                });

                final TimePicker addTime = (TimePicker) d.findViewById(R.id.addTime);
                addTime.setOnTimeChangedListener(new TimePickListen());

                Switch assignTime = (Switch) d.findViewById(R.id.assignTime);
                assignTime.setChecked(false);
                addTime.setVisibility(View.GONE);
                assignTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            addTime.setVisibility(View.VISIBLE);
                        } else addTime.setVisibility(View.GONE);
                    }
                });
            }
        });

        c = (CalendarView) findViewById(R.id.cal);
        c.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                new BackgroundHandler().execute(0, dayOfMonth, month, year);
            }
        });
        new BackgroundHandler().execute(0, dt.getDate(), dt.getMonth(), dt.getYear());

        taskview = (ListView) findViewById(R.id.todo_today);
    }

    private class TimePickListen implements TimePicker.OnTimeChangedListener {

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            String hr, min;
            if (minute < 10) {
                min = "0" + minute;
            } else min = minute + "";

            if (hourOfDay < 10) {
                hr = "0" + hourOfDay;
            } else hr = hourOfDay + "";

            time = hr + ":" + min;
            TextView t = (TextView) d.findViewById(R.id.taskTimeView);
            t.setText(time);
        }
    }

    static void deleteTask(int id,String taskName) {
        db.execSQL("delete from MyTasks where id = " + id);
        deleter.execute(3,taskName);
    }

    public class BackgroundHandler extends AsyncTask {
        TaskAdapter aa;

        @Override
        protected Object doInBackground(Object[] params) {
            int mode = (int) params[0];

            //Initially called to set next id
            if (mode == -1) {
                Cursor c = db.rawQuery("select * from MyTasks", null);
                nextId = c.getCount();
            }

            //For date change in calendar
            if (mode == 0) {
                int day = (int) params[1];
                int month = (int) params[2];
                int yr = (int) params[3];
                Date d = new Date(yr, month, day);
                dt = d;

                db.execSQL("create table if not exists MyTasks(id int,day int,month int,yr int,time varchar(5),task_detail varchar(100))");

                System.out.println("Retrieving at " + day + " " + month + " " + yr);
                Cursor result = db.rawQuery("select task_detail,time,id from MyTasks where day = ? and month = ? and yr = ?", new String[]{day + "", month + "", yr + ""});
                result.moveToFirst();

                int i = 0;
                v = new Vector<>();
                System.out.println(result.getCount());
                while (i != result.getCount()) {
                    Task t = new Task(result.getString(1), result.getString(0), result.getInt(2));
                    v.add(t);
                    result.moveToNext();
                    i++;
                }

                aa = new TaskAdapter(getApplicationContext(), R.layout.list_content, v);

                publishProgress(0, 1);
            }

            //Adding task
            if (mode == 1) {
                String name = (String) params[1];
                String time = (String) params[2];

                if(name.equals("")){
                    name = DEFAULT_EVENT_NAME;
                    publishProgress(1,1);
                }

                int day = dt.getDate(), month = dt.getMonth(), yr = dt.getYear();
                System.out.println("Adding task at " + day + " " + month + " " + yr + " " + time + " " + name);
                db.execSQL("insert into MyTasks values(" + nextId + "," + day + "," + month + "," + yr + ",'" + time + "','" + name + "')");
                nextId++;
                Log.d("Event Added to Database","Day = "+dt.getDate()+" Month = "+dt.getMonth()+" Year = "+dt.getYear());
                Log.d("Event Added to Database","ID = "+(nextId-1)+" and Name = "+name);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_CALENDAR},1);
                }
                else{
                    publishProgress(1,0);
                }
            }

            //Adding events to Calendar
            if(mode == 2){
                ContentResolver cr = getApplicationContext().getContentResolver();
                ContentValues values = new ContentValues();

                values.put(CalendarContract.Events.CALENDAR_ID, CALENDAR_ID); // id, We need to choose from
                // our mobile for primary
                // its 1
                values.put(CalendarContract.Events.TITLE, name);
                values.put(CalendarContract.Events.DESCRIPTION, EVENT_DESCRIPTION);

                int hr, min;
                if (time.equals("All Day")) {
                    hr = 0;
                    min = 1;
                    values.put(CalendarContract.Events.ALL_DAY, 1);
                } else {
                    hr = Integer.parseInt(time.substring(0, 2));
                    min = Integer.parseInt(time.substring(3));
                    values.put(CalendarContract.Events.ALL_DAY, 0);
                }

                GregorianCalendar cal = new GregorianCalendar(dt.getYear(), dt.getMonth(), dt.getDate(), hr, min);


                long startDate = cal.getTimeInMillis();
                long endDate = startDate + 1000 * 60 * 60;

                values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                values.put(CalendarContract.Events.DTSTART, startDate);
                values.put(CalendarContract.Events.DTEND, endDate);

                values.put(CalendarContract.Events.HAS_ALARM, 1); // 0 for false, 1 for true

                Uri baseUri;
                if (Build.VERSION.SDK_INT >= 8) {
                    baseUri = Uri.parse("content://com.android.calendar/events");
                } else {
                    baseUri = Uri.parse("content://calendar/events");
                }

                Uri uri = cr.insert(baseUri, values);
                long eventID = Long.parseLong(uri.getLastPathSegment());

                String reminderUriString = "content://com.android.calendar/reminders";

                ContentValues reminderValues = new ContentValues();

                reminderValues.put("event_id", eventID);
                reminderValues.put("minutes", 5); // Default value of the
                // system. Minutes is a
                // integer
                reminderValues.put("method", 1); // Alert Methods: Default(0),
                // Alert(1), Email(2),
                // SMS(3)

                Uri reminderUri = getApplicationContext().getContentResolver().insert(Uri.parse(reminderUriString), reminderValues);
                Date addedEventDate = cal.getTime();
                Log.d("Event added to calendar","Day = "+addedEventDate.getDate()+" Month = "+addedEventDate.getMonth()+" Year = "+addedEventDate.getYear());
                Log.d("Event added to calendar","ID = "+eventID+" Name = "+name);
            }

            //Deleting event from calendar
            else if(mode == 3){
                if(!hasPermission){
                    requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},2);
                    deleter = new BackgroundHandler();
                    return null;
                }
                String name = (String) params[1];
                ContentResolver resolver = getApplicationContext().getContentResolver();
                Cursor cursor;
                Uri eventsUri;

                int osVersion = android.os.Build.VERSION.SDK_INT;
                if (osVersion <= 7) { //up-to Android 2.1
                    eventsUri = Uri.parse("content://calendar/events");
                    cursor = resolver.query(eventsUri, new String[]{ "_id" }, "Calendars._id=" + CALENDAR_ID + " and Calendars.title='"+name+"' and description='"+EVENT_DESCRIPTION+"'" , null, null);
                } else { //8 is Android 2.2 (Froyo) (http://developer.android.com/reference/android/os/Build.VERSION_CODES.html)
                    eventsUri = Uri.parse("content://com.android.calendar/events");
                    cursor = resolver.query(eventsUri, new String[]{ "_id" }, "calendar_id=" + CALENDAR_ID + " and title='"+name+"' and description='"+EVENT_DESCRIPTION+"'" , null, null);
                }

                while (cursor.moveToNext()) {
                    long eventId = cursor.getLong(cursor.getColumnIndex("_id"));
                    resolver.delete(ContentUris.withAppendedId(eventsUri, eventId), null, null);
                    Log.d("Delete Event", "Deleted event with ID = " + eventId + " and Name = " + name);
                }
                cursor.close();
                deleter = new BackgroundHandler();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            int mode = (int) values[0];
            if(mode==0){
                int m = (int) values[1];
                if(m==1){
                    taskview.setAdapter(aa);
                }
            }
            else if(mode == 1){
                int submode = (int) values[1];
                if(submode==0)
                    new BackgroundHandler().execute(2);
                else if(submode==1)
                    Snackbar.make(findViewById(R.id.fab),"Event added successfully",Snackbar.LENGTH_LONG)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(TaskSelector.this, "Try this after next update", Toast.LENGTH_SHORT).show();
                                }
                            });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){

            //Permission asked when inserting event to calendar
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    new BackgroundHandler().execute(2);
                }
                break;

            //Permission asked when deleting event from calendar
            case 2:
                //No event has been added to calendar.
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.todaysTasks){
            Intent in = new Intent(getApplicationContext(),TodaysTasksActivity.class);
            startActivity(in);
        }

        return super.onOptionsItemSelected(item);
    }

    public class Task{
        String time,name;
        int id;

        Task(String time,String name,int id){
            this.time = time;
            this.name = name;
            this.id = id;
        }
    }
}
