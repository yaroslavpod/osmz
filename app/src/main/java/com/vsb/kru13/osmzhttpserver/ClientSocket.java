package com.vsb.kru13.osmzhttpserver;

import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;

public class ClientSocket extends Thread  {

    private Socket socketServer;
    private Handler messageHandler;
    private Message msg;
    private Bundle bundle;
    private Semaphore semaphore;

    public ClientSocket(Socket socketServer, Handler messageHandler, Semaphore semaphore) {
        this.socketServer = socketServer;
        this.messageHandler = messageHandler;
        this.semaphore =semaphore;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {
        try {
            try {
                sockets();
            }catch (NullPointerException e){
                try {
                    socketServer.close();
                }catch (NoSuchElementException el){
                    el.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            msg.setData(bundle);
            messageHandler.sendMessage(msg);
            semaphore.release();
            Log.d("THREADS", "Free lock " + semaphore.availablePermits());

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void sockets() throws IOException,NullPointerException, NoSuchElementException {

        OutputStream o = socketServer.getOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
        // we read characters from the client via input stream on the socket
        BufferedReader in = new BufferedReader(new InputStreamReader(socketServer.getInputStream()));
        // we get character output stream to client (for headers)
        DataOutputStream outToClient = new DataOutputStream(socketServer.getOutputStream());
        // get first line of the request from the client
        String tmp = in.readLine();
        String fileRequested = null;
        // we parse the request with a string tokenizer
        StringTokenizer parse = new StringTokenizer(tmp);
        String method = parse.nextToken().toUpperCase();
        // we get file requested
        fileRequested = parse.nextToken().toLowerCase();
        String sdPath = "/sdcard/http";
        msg = messageHandler.obtainMessage();
        bundle = new Bundle();
        try {

            while (!tmp.isEmpty()) {
                Log.d("SERVER", "MED:" + tmp);
                tmp = in.readLine();
            }

            if (!method.equals("GET")) {
                File file = new File(sdPath + "/not_supported.html");
                int fileLength = (int) file.length();


                String response = "HTTP/1.1 501 Not Implemented\r\n" +
                        "Server: nginx\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + (int) file.length() + "\r\n" +
                        "Connection: close\r\n\r\n";
                out.write(response);
                out.flush();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];

                int len;
                while((len = fileInputStream.read(buffer)) != -1)
                {
                    o.write(buffer, 0, len);
                }

                bundle.putString("date", getCurrentTime());
                bundle.putString("HTTP", method);
                bundle.putString("/", fileRequested);
                bundle.putString("0", fileLength+"");


            } else {

                if(fileRequested.contains("cgi-bin")){
                    String cgiCommand = "";
                    Log.d("CGI request", fileRequested);
                    int cgiIndex = fileRequested.indexOf("cgi-bin")+"cgi-bin".length()+1;
                    int endIndex =   fileRequested.length();

                    String encodedCommandToExecute = fileRequested.substring(cgiIndex, endIndex);


                    StringBuilder decodedCommandToExecute = new StringBuilder();
                    decodedCommandToExecute.append(URLDecoder.decode(encodedCommandToExecute, "UTF-8"));
                    Log.d("CGI decoded command", decodedCommandToExecute.toString());

                    cgiCommand = decodedCommandToExecute.toString();
                    cgiBuilder(cgiCommand,sdPath,out,fileRequested);
                    socketServer.close();
                    return;

                }

                if (fileRequested.endsWith("/")) {
                    fileRequested += "index.html";
                }
                File file = new File(sdPath + fileRequested);

                if(!file.exists()) {
                    throw new FileNotFoundException();
                }




                String response = "HTTP/1.1 200 OK\r\n" +
                        "Server: nginx\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + (int) file.length() + "\r\n" +
                        "Connection: close\r\n\r\n";
                out.write(response);
                out.flush();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                while((len = fileInputStream.read(buffer)) != -1)
                {
                    o.write(buffer, 0, len);
                }

                bundle.putString("date", getCurrentTime());
                bundle.putString("HTTP", method);
                bundle.putString("/", fileRequested);
                bundle.putString("0", (int) file.length()+"");
            }
                socketServer.close();
        }catch (FileNotFoundException e) {
            try {
                fileNotFound(out, outToClient,sdPath,fileRequested,method);
                socketServer.close();
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        }


    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void fileNotFound(BufferedWriter out, OutputStream o, String sdPath, String fileRequested,String method) throws IOException {
        File fileNotFound = new File(sdPath + "/" + "404.html");


        if (fileRequested.endsWith(".html")) {

            String respHtml = "<html>\n" +
                    "<body>\n" +
                    "<h1>You are on  " +fileRequested.replaceAll("/","")+"</h1>\n" +
                    "</body>\n" +
                    "</html>";

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Server: nginx\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length:" +respHtml.length()+"\r\n" +
                    "Connection: close\r\n\r\n";


            out.write(response+respHtml);
            out.flush();
            bundle.putString("date", getCurrentTime());
            bundle.putString("http", method);
            bundle.putString("/", fileRequested);
            bundle.putString("0", respHtml.length()+"");
            return;
        }

        String response = "HTTP/1.1 404 Page Not Found\r\n" +
                "Server: nginx\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + (int) fileNotFound.length() + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(response);
        out.flush();
        FileInputStream fileInputStream = new FileInputStream(fileNotFound);
        byte[] buffer = new byte[1024];

        int len;
        while((len = fileInputStream.read(buffer)) != -1)
        {
            o.write(buffer, 0, len);
        }
        out.close();
        o.close();
        bundle.putString("date", getCurrentTime());
        bundle.putString("http", method);
        bundle.putString("/", fileRequested);
        bundle.putString("0", (int) fileNotFound.length()+"");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss",Locale.GERMANY);
        return dateFormat.format(new Date());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void cgiBuilder(String cgiCommand, String sdPath, BufferedWriter out,String fileRequested){
            StringBuilder cgiStringBuilder = new StringBuilder();
            try {
                File cgiDir = new File(sdPath + "/CGI");
                File file = new  File(sdPath + "/CGI/cgi.txt");
                if (! cgiDir.exists()){
                    if (! cgiDir.mkdirs()){
                        Log.d("CGI", "failed to create directory");
                        return;
                    }
                }
                ProcessBuilder pb;
                if(cgiCommand.contains("cat")){
                    pb = new ProcessBuilder("cat", cgiCommand.split(" ")[1]);
                } else {
                    pb = new ProcessBuilder(cgiCommand);
                }
                final Process p = pb.start();

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedWriter bw = new BufferedWriter(
                        new FileWriter(file));

                String line;
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                    cgiStringBuilder.append(line + "\n");
                }
                bw.close();


                if(cgiStringBuilder.length() > 0) {
                    Log.d("CGI OUTPUT", cgiStringBuilder.toString());


                    out.write("HTTP/1.0 200 OK\n");
                    out.write("CGI OUTPUT:\n\n");
                    out.write(cgiStringBuilder.toString());
                    out.flush();
                } else {
                    Log.d("CGI EMPTY", "Requested command is not valid to extract");
                    out.write("HTTP/1.0 404 Not Found\n\n");
                    out.write("CGI OUTPUT ERROR:\n\n");
                    out.flush();
                }
                bundle.putString("date", getCurrentTime());
                bundle.putString("http", "GET");
                bundle.putString("/", fileRequested);
                bundle.putString("0", (int)file.length()+"");
            } catch (Exception ex) {
                System.out.println(ex);
            }

    }

}
