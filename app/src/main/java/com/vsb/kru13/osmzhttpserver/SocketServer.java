package com.vsb.kru13.osmzhttpserver;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import static com.vsb.kru13.osmzhttpserver.HTTPServerActivity.mCamera;

public class SocketServer extends Thread {

    private ServerSocket serverSocket;
    public final int port = 12345;

    boolean bRunning;

    private Handler mHandler;
    private Semaphore semaphore;

    SocketServer(Handler messageHandler,int threadCount) {
        this.mHandler = messageHandler;
        this.semaphore = new Semaphore(threadCount);
    }



    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;

            while (bRunning) {

                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                try{
                    semaphore.acquire();
                    Log.d("THREADS", "Get LOCK " + semaphore.availablePermits());
                    ClientSocket clientSocket = new ClientSocket(s,mHandler,semaphore);
                    clientSocket.start();
                }catch(InterruptedException e){
                    Log.d("SERVER", "No free slots");
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket = null;
            bRunning = false;
        }
    }
    public void close() {
        try {
            serverSocket.close();
            mCamera.setPreviewCallback(null);
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }
}

