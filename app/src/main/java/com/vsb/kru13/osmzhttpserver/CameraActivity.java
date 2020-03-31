package com.vsb.kru13.osmzhttpserver;

import android.hardware.Camera;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Environment;


public class CameraActivity implements Camera.PictureCallback {
    private File img;
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d("Camera", "Image captured.");
        if (data == null)
            return;
        img = getOutputMediaFile();

        if (img == null){
            Log.d("Camera", "Error creating media file, check storage permissions");
            return;
        }

        try {
            FileOutputStream fout = new FileOutputStream(img);
            fout.write(data);
            fout.close();
        } catch (FileNotFoundException e) {
            Log.d("Camera", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("Camera", "Error accessing file: " + e.getMessage());
        }
        finally {
            camera.startPreview();
        }

    }
    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "safe");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("AMyCameraApp", "Failed to create directory, check permissions");
                return null;
            }
        }

        File file;
        file = new File(mediaStorageDir.getPath() + File.separator + "image.jpg");
        return file;
    }


}
