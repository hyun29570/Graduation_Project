package com.example.graduation_project;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.util.FusedLocationSource;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback, SensorEventListener
{
    private MapView mapView;
    private NaverMap mNaverMap;
    private FusedLocationSource mLocationSource;
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    List<LatLng> lstLatLng = new ArrayList<>();
    private Chronometer chronometer_t;
    private boolean running;
    private long pauseOffset;
    private Button startBtn, stopBtn, resetBtn, CapBtn, endBtn;
    //--------------------------------------
    SensorManager sensorManager;
    Sensor stepCountSensor;
    TextView stepCountView;
    int currentSteps = 0;
    //--------------------------------------
    CalendarFragment fragment;

    public MapFragment() { }

    public static MapFragment newInstance()
    {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_map,
                container, false);

        mapView = (MapView) rootView.findViewById(R.id.navermap);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mLocationSource = new FusedLocationSource(this,PERMISSION_REQUEST_CODE);

        chronometer_t = rootView.findViewById(R.id.et_placeName);
        chronometer_t.setFormat("??????: %s");
        //------------------------?????????

        stepCountView = rootView.findViewById(R.id.et_placeStep);
        stepCountView.setText("?????? ??? : "+currentSteps);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        //------------------------

        //------------------------?????? ??????
        fragment = new CalendarFragment();

        endBtn = rootView.findViewById(R.id.sendCButton);
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putInt("step", currentSteps);
                bundle.putInt("t", 1);
                fragment.setArguments(bundle);
                /*
                Intent intent = new Intent();
                intent.putExtra("steps",currentSteps);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();

                 */
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.main_layout,fragment).commitAllowingStateLoss();
            }
        });
        //------------------------

        startBtn = rootView.findViewById(R.id.startbutton);
        stopBtn = rootView.findViewById(R.id.stopbutton);
        resetBtn = rootView.findViewById(R.id.resetbutton);
        CapBtn = rootView.findViewById(R.id.capbutton);

        CapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capture();
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!running) {
                    chronometer_t.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                    chronometer_t.start();
                    running = true;
                    //-----------------
                    s_onStart();
                    //-----------------
                }

            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(running) {
                    chronometer_t.stop();
                    pauseOffset = SystemClock.elapsedRealtime() - chronometer_t.getBase();
                    running = false;
                }

            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    chronometer_t.setBase(SystemClock.elapsedRealtime());
                    pauseOffset = 0;
                    //-------------------
                    currentSteps = 0;
                    stepCountView.setText("?????? ??? : "+currentSteps);
                    //-----------------------------------------
            }
        });

        return rootView;
    }

    public void capture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 101);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        //?????? ?????? ??????
        mNaverMap = naverMap;
        naverMap.setLocationSource(mLocationSource);
        requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setCompassEnabled(true);
        uiSettings.setScaleBarEnabled(true);
        uiSettings.setZoomControlEnabled(true);
        uiSettings.setLocationButtonEnabled(true);
        uiSettings.setLogoGravity(Gravity.LEFT|Gravity.BOTTOM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(mLocationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if(!mLocationSource.isActivated()) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
                return;
            } else{
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStart()
    {
        String addr;

        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    //--------------------------?????????
    public void s_onStart() {
        super.onStart();
        if (stepCountSensor == null) {
            Toast.makeText(getActivity().getApplicationContext(), "No Step Sensor", Toast.LENGTH_SHORT).show();
        }
        else if(stepCountSensor !=null) {
            // ?????? ?????? ??????
            // * ??????
            // - SENSOR_DELAY_NORMAL: 20,000 ??? ?????????
            // - SENSOR_DELAY_UI: 6,000 ??? ?????????
            // - SENSOR_DELAY_GAME: 20,000 ??? ?????????
            // - SENSOR_DELAY_FASTEST: ????????? ??????
            //
            sensorManager.registerListener(this,stepCountSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // ?????? ?????? ????????? ?????????
        if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR && running == true){

            switch(event.sensor.getType()){
                // ?????? ???????????? ???????????? ?????? ????????? ??????
                case Sensor.TYPE_STEP_DETECTOR :
                currentSteps++;
                stepCountView.setText("?????? ??? : "+currentSteps);
            }

        }

    }



    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //--------------------------



}