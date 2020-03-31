package com.vsb.kru13.osmzhttpserver;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.hardware.Camera;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.Manifest;
import android.content.Intent;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.view.View;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.widget.EditText;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Button;


public class HTTPServerActivity extends AppCompatActivity implements View.OnClickListener {


    private int threadCount;

    private SocketServer socketServer;
    private static final int READ_EXTERNAL_STORAGE = 1;
    private String setText;
    TextView threadCounter;
    String count;
    TextView textView;
    private EditText editText;


    private FrameLayout previewLayout;
    private Intent service;
    private ServiceActivity serviceActivity ;
    boolean mBound = false;


    public static final Camera mCamera = getCameraInstance();

    public static CameraPreview preview;
    private CameraActivity cameraActivity;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        threadCounter = (TextView) findViewById(R.id.threadCount);

        threadCount = Integer.parseInt(editText.getText().toString());

        count = "5";
        setText = "";

        textView = (TextView) findViewById(R.id.textView);
        editText = (EditText)findViewById((R.id.editText));
        textView.setText("Threads");

        Button serviceStartBtn = (Button)findViewById(R.id.serviceStartBtn);
        serviceStartBtn.setOnClickListener(this);
        Button btn2 = (Button)findViewById(R.id.button2);
        btn2.setOnClickListener(this);


        Button shutterBtn = (Button)findViewById(R.id.shutterBtn);

        preview = new CameraPreview(this,mCamera);
        previewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        previewLayout.addView(preview);



    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.serviceStartBtn) {

            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
            } else {
                setText +="Start Service\n";
                threadCounter.setText(setText);
                service = new Intent(this,ServiceActivity.class);
                startService(service);
                bindService(service, mConnection, Context.BIND_AUTO_CREATE);
            }
        }
        //Stop
        if (v.getId() == R.id.button2) {
            if (mBound) {
                unbindService(mConnection);
                mBound = false;
            }
            stopService(service);
            setText +="Stop Service\n";
            threadCounter.setText(setText);
        }
        if (v.getId() == R.id.shutterBtn) {
            Log.d("CAMERA", "Taking picture...");
            mCamera.takePicture(null, null, cameraActivity);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
            serviceActivity = ((ServiceActivity.LocalBinder)iBinder).getService();
            serviceActivity.setSettings(messageHandler,threadCount);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
        }
        return c;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    socketServer = new SocketServer(messageHandler,threadCount);
                    socketServer.start();
                }
                break;

            default:
                break;
        }
    }
    private static final String MSG_DATA = "date";
    private static final String MSG_HTTP= "GET";
    private static final String MSG_NAME   = "/";
    private static final String MSG_SIZE= "0";
    @SuppressLint("HandlerLeak")
    private Handler messageHandler = new Handler() {

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            setText += bundle.getString(MSG_DATA) +" "+bundle.getString(MSG_HTTP) +" "+bundle.getString(MSG_NAME)+" "+bundle.getString(MSG_SIZE)+" byte"+"\n";
            threadCounter.setText(setText);


        }
    };






}
