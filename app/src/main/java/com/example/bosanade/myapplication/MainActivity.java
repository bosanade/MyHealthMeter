package com.example.bosanade.myapplication;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener, LocationListener {
    protected SensorManager mSensorManager;
    private TextView mStepsTextView;
    private ImageButton mStartStopButton;
    private float mPrevCount;
    private float mCount;
    private ProgressBar mProgressBar;

    private Timer timer;
    private CountUpTimerTask timerTask;
    private Handler handler = new Handler();

    private TextView timerText;
    private long count, delay, period;

    double latitude1 = 0.0;
    double longitude1 = 0.0;
    double latitude2 = 0.0;
    double longitude2 = 0.0;

    float[] distance;

    LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pedometer);

        //センサー
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //タイマー
        mStepsTextView = (TextView) findViewById(R.id.timer);
        //スタート・ストップ
        mStartStopButton = (ImageButton) findViewById(R.id.button1);
        mStartStopButton.setOnClickListener(this);

        //プログレスバー
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        // 水平プログレスバーの最大値を設定します
        mProgressBar.setMax(100);
        // 水平プログレスバーの値を設定します
        mProgressBar.setProgress(100);

        // タイマー
        timerText = findViewById(R.id.timer);
        delay = 0;
        period = 100;

        //位置情報
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 50, this);
        }
    }

    //位置情報
    public float[] getDistance(double x, double y, double x2, double y2) {
        // 結果を格納するための配列を生成
        float[] results = new float[3];

        // 距離計算
        Location.distanceBetween(x, y, x2, y2, results);

        return results;
    }

    private void locationStart() {
        Log.d("debug", "locationStart()");

        // LocationManager インスタンス生成
        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d("debug", "location manager Enabled");
        } else {
            // GPSを設定するように促す
            Intent settingsIntent =
                    new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
            Log.d("debug", "not gpsEnable, startActivity");
        }

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);

            Log.d("debug", "checkSelfPermission false");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 50, this);

    }

    //センサー動作
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (mPrevCount != 0) {
                mCount += event.values[0] - mPrevCount;
            }
            mPrevCount = event.values[0];
        } else {
            mCount++;
        }
        mStepsTextView.setText("実行中\n" + (int) mCount);
        mProgressBar.setProgress((int) (100 - mCount));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing to do
    }

    @Override
    public void onClick(View v) {
        Object tag = mStartStopButton.getTag();
        int resid;

        if (Integer.valueOf(android.R.drawable.ic_media_pause).equals(tag)) {
            //アイコンチェンジ
            resid = android.R.drawable.ic_media_play;
            // タイマーストップ
            timer.cancel();
            timer = null;
        } else {
            // Timer インスタンスを生成
            timer = new Timer();
            // TimerTask インスタンスを生成
            timerTask = new CountUpTimerTask();

            int type = Sensor.TYPE_STEP_COUNTER;
            Sensor sensor = mSensorManager.getDefaultSensor(type);
            //アイコンチェンジ
            resid = android.R.drawable.ic_media_pause;
            // public void schedule (TimerTask task, long delay, long period)
            timer.schedule(timerTask, delay, period);
            // カウンター
            count = 0;
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
        mStartStopButton.setImageResource(resid);
        mStartStopButton.setTag(resid);
    }

    //タイマー
    class CountUpTimerTask extends TimerTask {
        @Override
        public void run() {
            // handlerを使って処理をキューイングする
            handler.post(new Runnable() {
                public void run() {
                    count++;
                    long mm = count * 100 / 1000 / 60;
                    long ss = count * 100 / 1000 % 60;
                    long ms = (count * 100 - ss * 1000 - mm * 1000 * 60) / 100;
                    // 桁数を合わせるために02d(2桁)を設定
                    timerText.setText(
                            String.format(Locale.US, "%1$02d:%2$02d.%3$01d", mm, ss, ms));
                }
            });
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                Log.d("debug", "LocationProvider.AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // 緯度
        Double str1 = location.getLatitude();

        // 経度
        double str2 = location.getLongitude();

        if (latitude1 != str1 && longitude1 != str2) {
            latitude1 = str1;
            longitude1 = str2;
        } else {
            latitude2 = str1;
            longitude2 = str2;
            distance = getDistance(latitude1, longitude1, latitude2, longitude2);

            TextView textView3 = (TextView) findViewById(R.id.location);
            double str3 = distance[0];
            textView3.setText(String.valueOf(Math.floor(str3)));
        }
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}