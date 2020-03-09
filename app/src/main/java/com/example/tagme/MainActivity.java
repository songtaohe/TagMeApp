package com.example.tagme;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends AppCompatActivity {

    LocationManager mLocationManager;
    long basetime;
    long basetime_global;
    MyLogger myLogger;
    boolean isLogging;
    String uploadState = "None";
    String globalinfo = "None";
    String logname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        this.basetime = this.getTime();
        this.myLogger = new MyLogger();
        this.isLogging = false;
        uploadState = "Not Synced";

        // Text view

        final TextView log_text = (TextView) findViewById(R.id.textViewState);
        final TextView state_text = (TextView) findViewById(R.id.textViewLog);


        // buttons

        Button buttonstart = (Button) findViewById(R.id.buttonstart);
        Button buttonstop = (Button) findViewById(R.id.buttonstop);
        Button buttonupload = (Button) findViewById(R.id.buttonupload);
        Button buttoncheck = (Button) findViewById(R.id.buttoncheck);


        buttonstart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                basetime = getTime();
                basetime_global = System.currentTimeMillis();
                myLogger.newLog();
                logname = getDate();
                isLogging = true;
            }
        });

        buttonstop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //myLogger.newLog();
                //logname = getDate();
                isLogging = false;
                uploadState = "Not Synced";
            }
        });

        final RequestQueue MyRequestQueue = Volley.newRequestQueue(this);

        buttonupload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //myLogger.newLog();
                //logname = getDate();
                //isLogging = false;

                String json = myLogger.getJsonString();

                JSONObject jo = myLogger.getJsonObject();

                EditText editText=findViewById(R.id.editText);


                try {
                    jo.put("cmd", "save");
                    jo.put("name", logname);
                    jo.put( "basetime", basetime_global);
                    jo.put("note", editText.getText().toString());
                } catch (Exception e) {

                }

                // save to local internal storage
                String filename = logname;
                String fileContents = jo.toString();
                try {
                    FileOutputStream fos = getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE);
                    fos.write(fileContents.getBytes());
                } catch (Exception e) {

                }


                uploadState = "Syncing...";
                JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, "http://18.26.2.99:8008", jo,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //This code is executed if the server responds, whether or not the response contains data.
                        //The String 'response' contains the server's response.
                        //Log.v("onResponse",response);
                        uploadState = "Synced!";
                    }
                }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //This code is executed if there is an error.
                        Log.v("onError",error.toString());
                        uploadState = "Synced with error return";
                    }
                });

                MyRequestQueue.add(req);

            }


        });

        buttoncheck.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] files = getApplicationContext().fileList();

                String s = "";
                for (int i=0;i<files.length;i++) {
                    Log.v("hstcheckfilelist", files[i]);
                    if (i < files.length-5) {
                        s += i + ": "+ files[i]+"\n";
                    }
                }

                log_text.setText(s);

                // talk to server to get a list
                JSONObject jo = new JSONObject();
                try {
                    jo.put("cmd", "getlist");
                } catch (Exception e) {

                }

                JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, "http://18.26.2.99:8008", jo,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                            try {
                                JSONArray ja = response.getJSONArray("filelist");
                                Hashtable filemap = new Hashtable();
                                //JSONParser parser = new JSONParser();

                                for (int i = 0; i < ja.length(); i++ ) {
                                    filemap.put(ja.getString(i), "exist");
                                }

                                String unsync_s = "";
                                for (int i=0;i<files.length;i++) {
                                    if (filemap.containsKey(files[i]) == false) {
                                        // need to sync
                                        unsync_s += files[i] + " (not synced)\n";

                                        // load the file
                                    } else {
                                        unsync_s += files[i] + " (synced)\n";
                                    }
                                }
                                log_text.setText(unsync_s);


                            } catch (Exception e) {

                            }
                            }
                        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //This code is executed if there is an error.
                        Log.v("onError",error.toString());
                    }
                });

                MyRequestQueue.add(req);




                //myLogger.newLog();
                //logname = getDate();
                //isLogging = false;
            }
        });

        // Internet

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    1);
        }


        // GPS
        this.mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        ((MyLocationListener) locationListener).mMainActivity = this;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }


        this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);


        // Sensors
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Sensor sensor1 = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor sensor1b = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor sensor1c = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor sensor2 = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor sensor3 = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        //Sensor sensor4 = mSensorManager.getDefaultSensor(Sensor.);

        SensorEventListener sensorEventListener = new MySensorListener();

        ((MySensorListener) sensorEventListener).mMainActivity = this;

        mSensorManager.registerListener(sensorEventListener, sensor1, 10000);
        mSensorManager.registerListener(sensorEventListener, sensor1b, 10000);
        mSensorManager.registerListener(sensorEventListener, sensor1c, 10000);
        mSensorManager.registerListener(sensorEventListener, sensor2, 10000);
        mSensorManager.registerListener(sensorEventListener, sensor3, 10000);


        class checkState extends TimerTask {
            public MainActivity mMainActivity;
            public void run() {
                String s = "";

                s += "== Logging State == \n";
                s += "isLogging?  " + isLogging + " \n";
                s += "isUploaded?  " + uploadState + " \n";
                s += "\n";
                s += "== Sensor Frequency == \n";
                s += myLogger.getFreqStr();
                s += "\n";
                s += "== Misc ==\n";
                s += "LogFileName: " + logname + "\n";
                s += "Time(ms): " + getTimeFromStart() + "\n";
                s += "Log Size: " + myLogger.getLogSize() + "\n";
                s += "\n";
                s += globalinfo + "\n";

                //s += "T(ms): " + getTimeFromStart() + "\n";

                state_text.setText(s);
            }
        }

        Timer timer = new Timer();

        TimerTask checkStateTask = new checkState();
        timer.scheduleAtFixedRate(checkStateTask, 1000, 1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        return super.onOptionsItemSelected(item);
    }

    public long getTime() {
        long time= System.currentTimeMillis();
        return time;
    }

    public long getTimeFromStart() {
        long time= System.currentTimeMillis();
        return time - this.basetime;
    }

    public String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date currentTime = Calendar.getInstance().getTime();

        return dateFormat.format(currentTime);
    }



    public class MyLogger {
        List<String> loglist;
        Lock mLock;

        Hashtable lastts = new Hashtable();
        Hashtable freq = new Hashtable();


        public MyLogger() {
            Log.v("hst",">>>???<<<");
            this.loglist = new ArrayList<>();
            this.mLock = new ReentrantLock();
            //Log.v("hst",mLock.toString());
        }

        public void addLogEntry(String s) {
            //this.loglist.add(s);

            mLock.lock();
            try {
                this.loglist.add(s);

                String[] items = s.split(",");
                long ts = Long.parseLong(items[0]);
                //Log.v("hst--", items[0] + "___" + items[1]);
                if (lastts.containsKey(items[1]) == false) {
                    lastts.put(items[1], ts);

                } else {
                    //Log.v("hst--", items[0] + "___" + items[1]);
                    long last_ts = (long)lastts.get(items[1]);
                    freq.put(items[1], ts - last_ts);
                    lastts.put(items[1], ts);
                }
                //Log.v("hst","error!");
            } finally {

                mLock.unlock();
            }
        }

        public String getJsonString() {
            JSONArray mJSONArray;
            mLock.lock();
            try {

                mJSONArray = new JSONArray(loglist);
            } finally {
                mLock.unlock();
            }

            return mJSONArray.toString();
        }

        public JSONObject getJsonObject() {
            JSONArray mJSONArray;
            mLock.lock();
            try {

                mJSONArray = new JSONArray(loglist);
            } finally {
                mLock.unlock();
            }
            JSONObject jo = new JSONObject();
            try {
                jo.put("data", mJSONArray);
            } catch (Exception e) {

            }

            return jo;
        }

        public void newLog() {
            mLock.lock();
            try {
                this.loglist.clear();
            } finally {
                mLock.unlock();
            }
        }

        public String getFreqStr() {
            String s = "";
            mLock.lock();
            try {
                for (Object key_s: freq.keySet()) {
                    String key = (String)key_s;
                    s += "Sensor: "+ key + " Interval: " + freq.get(key) + " ms\n";
                }

            } finally {
                mLock.unlock();
            }

            return s;
        }

        public int getLogSize() {
            int s = 0;

            mLock.lock();
            try {
                s = this.loglist.size();
            } finally {
                mLock.unlock();
            }

            return s;
        }
    }


    /*---------- Listener class to get coordinates ------------- */
    public class MyLocationListener implements LocationListener {
        public MainActivity mMainActivity;

        @Override
        public void onLocationChanged(Location loc) {
            //editLocation.setText("");
            //pb.setVisibility(View.INVISIBLE);
//            Toast.makeText(
//                    getBaseContext(),
//                    "Location changed: Lat: " + loc.getLatitude() + " Lng: "
//                            + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            //String longitude = "Time " + mMainActivity.getTimeFromStart() + "GPS: " + loc.getLatitude() + " "+loc.getLongitude();
            //Log.v("hst", longitude);

            if (mMainActivity.isLogging == true) {
                String s = mMainActivity.getTimeFromStart() + ",GPS,"+ loc.getLatitude() + ","+loc.getLongitude()+","+loc.getAccuracy()+","+loc.getBearing()+","+loc.getSpeed()+","+loc.getProvider();
                mMainActivity.myLogger.addLogEntry(s);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }



    public class MySensorListener implements SensorEventListener {
        public MainActivity mMainActivity;

        @Override
        public void onSensorChanged(SensorEvent event) {


            if (mMainActivity.isLogging == true) {
//                String data = "Time " + mMainActivity.getTimeFromStart()+ " Sensor Type: " + event.sensor.getName();
//                Log.v("hst",data);

                String value_str = "";
                for (int i = 0; i< event.values.length; i++) {
                    value_str = value_str + event.values[i] + ",";
                }
                String s = mMainActivity.getTimeFromStart() + ",Sensor"+event.sensor.getType()+","+ value_str;
                //Log.v("log", s);
                mMainActivity.myLogger.addLogEntry(s);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int event) {

        }
    }











}
