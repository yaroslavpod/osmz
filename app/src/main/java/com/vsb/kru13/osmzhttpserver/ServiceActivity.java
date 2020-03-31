package com.vsb.kru13.osmzhttpserver;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.Nullable;

public class ServiceActivity extends Service {
    private SocketServer socketServer;
    public Intent intent;
    private final IBinder mIBinder = new LocalBinder();


    @Override
    public void onCreate() {
        Log.d("Service", "Service created");
    }

    @Override
    public void onDestroy() {
        socketServer.close();
        Log.d("Service", "Service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }

    public class LocalBinder extends Binder {
        ServiceActivity getService() {
            return ServiceActivity.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        Log.d("SERVICE", "Service Started");
        return super.onStartCommand(intent, flags, startId);
    }

    public void setSettings(Handler handler, int thread) {
        socketServer = new SocketServer(handler, thread);
        socketServer.start();

    }
}

