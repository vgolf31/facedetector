/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mine.facedetector;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.annotation.KeepName;
import com.mine.facedetector.RetrofitStuff.ImageApi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Live preview demo for ML Kit APIs.
 */
@KeepName
public final class LivePreviewActivity extends AppCompatActivity {
    private ArrayList<String> imageFilePaths = new ArrayList<>();
    private ArrayList<String> previewImagePaths = new ArrayList<>();
    private ArrayList<String> sentImages = new ArrayList<>();
    private static final String TAG = "LivePreviewActivity";
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private Boolean isFrontFacing = true;
    private ImageView imgCameraCapture;
    private ImageView imgCapture;
    private ImageView imgDone;
    private boolean isPhotoDetected = false;
    private Calendar curdate = Calendar.getInstance();
    private Calendar calendar = Calendar.getInstance();
    private Calendar curdateRetro = Calendar.getInstance();
    private Calendar calendarRetro = Calendar.getInstance();
    private Calendar curdateRetro2 = Calendar.getInstance();
    private Calendar calendarRetro2 = Calendar.getInstance();
    private Calendar curdateRetro3 = Calendar.getInstance();
    private Calendar calendarRetro3 = Calendar.getInstance();
    public static boolean isPhotoClicked = false;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private int imageIndex = 0;
    private boolean retroIsDone = true;
    private ArrayList<Call> ongoingCalls = new ArrayList<>();

    Bitmap bitmap;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000;

    Context mContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_vision_live_preview);

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }
        //FACE_DETECTION
        ActivityCompat.requestPermissions(LivePreviewActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        ActivityCompat.requestPermissions(LivePreviewActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        ImageView facingSwitch = findViewById(R.id.facing_switch);
        imgCameraCapture = findViewById(R.id.imgCameraCapture);
        imgCapture = findViewById(R.id.imgCapture);
        imgDone = findViewById(R.id.imgDone);

        facingSwitch.setOnClickListener(v -> {
          isFrontFacing = !isFrontFacing;
          toggleCamera();
        });

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View view) {
                cancelRequests();
                Intent i = new Intent(mContext,MainActivity.class);
                startActivity(i);
            }
        });

        imgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bitmap != null){
                    Intent intent = new Intent(LivePreviewActivity.this, FullImageView.class);
                    startActivity(intent);
                }
            }
        });


        imgDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bitmap != null){
                    Intent intent = new Intent(LivePreviewActivity.this, FullImageView.class);
                    startActivity(intent);
                }
            }
        });

        imgCameraCapture.setOnClickListener(v -> {
            if(isPhotoDetected){
                isPhotoClicked = true;
                bitmap = loadBitmapFromView(graphicOverlay);
                imgCapture.setImageBitmap(bitmap);
                imgCapture.setVisibility(View.VISIBLE);
                imgDone.setVisibility(View.VISIBLE);
                createImageFromBitmap(bitmap);
            }else{
                Toast.makeText(this, "Please capture image only!", Toast.LENGTH_SHORT).show();
            }
            try {
                screenshot();
                Log.d("JAIDEN", "SCREENSHOT");
            } catch (IOException e) {
                e.printStackTrace();
            }

        });


        createCameraSource();
        toggleCamera();

//        String path = Environment.getExternalStorageDirectory().toString()+"/Download";
//        Log.d("JAIDEN", "Path: " + path);
//        File directory = new File(path);
//        Log.d("JAIDEN", ""+directory.getName());
//        File[] files = directory.listFiles();
//        Log.d("JAIDEN", "Size: "+ files);
//        for (int i = 0; i < files.length; i++)
//        {
//            Log.d("JAIDEN", "FileName:" + files[i].getName());
//        }
//        Log.d("JAIDEN", System.getProperty("user.dir"));
////        File testFile = new File("MainActivity.java");
////        Log.d("JAIDEN", "HI: "+testFile.getParent());
//
//        String filePath = Environment.getExternalStorageDirectory()+"/Download/"+ "" +".jpg";
//        File f = new File("");
        /*Runnable everySecond = new Runnable(){
            public void run(){
                try {
                    while (true) {
                        Log.d("JAIDEN", "run: "+imageFilePaths.size());
                        //upload images
                        retrofitMultiFileUpload();
                        File directory = new File(Environment.getExternalStorageDirectory()+"/Download/");
                        //delete files
                        for (File file: Objects.requireNonNull(directory.listFiles())) {
                            if (!file.isDirectory()) {
                                file.delete();
                            }
                        }
                        imageFilePaths.clear();
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {

                }

//                for (String filePath: imageFilePaths) {
//                    File file = new File(filePath);
//                    file.delete();
//                }
//
            }
        };*/
//        executor.scheduleAtFixedRate(everySecond, 0, 1, TimeUnit.SECONDS);
//        everySecond.run();
        /*
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
           @Override
           public void run () {
               Log.d("JAIDEN", "run: "+imageFilePaths.size());
               //upload images
               retrofitMultiFileUpload();
               File directory = new File(Environment.getExternalStorageDirectory()+"/Download/");
               //delete files
               for (File file: Objects.requireNonNull(directory.listFiles())) {
                   if (!file.isDirectory()) {
                       file.delete();
                   }
               }
               imageFilePaths.clear();
           }
        }, 0, 1000);
        */
    }

    public void cancelRequests () {
        for (Call c: ongoingCalls) {
            try {
                c.cancel();
            } catch (Exception e) {
                // do nothing
            }
        }
        retroIsDone = true;
        ongoingCalls.clear();
    }

    public String createImageFromBitmap(Bitmap bitmap) {
        String fileName = "myImage";//no .png or .jpg needed
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            FileOutputStream fo = openFileOutput(fileName, Context.MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            // remember close file output
            fo.close();
        } catch (Exception e) {
            e.printStackTrace();
            fileName = null;
        }
        return fileName;
    }

    private void toggleCamera(){
      Log.d(TAG, "Set facing");
      if (cameraSource != null) {
        if (isFrontFacing) {
          cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
        } else {
          cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
        }
      }
      preview.stop();
      startCameraSource();
    }


    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }
        cameraSource.setMachineLearningFrameProcessor(new FaceDetectorProcessor(this, new OnFaceDetectedListener() {
            @Override
            public void onFaceDetected(Boolean isDetected) throws IOException {
                isPhotoDetected = isDetected;
                if(isDetected){
                    Log.d("ABC","Face Detected");
                    calendar = Calendar.getInstance();
                    Calendar compare = (Calendar)curdate.clone();
                    compare.add(Calendar.MILLISECOND, 150);
                    imgCameraCapture.setImageResource(R.drawable.ic_camera_capture);
                    if(calendar.compareTo(compare) > 0){
                        Log.d("ABC","Taking screenshot");
                        curdate = Calendar.getInstance();
                        screenshot();
                    }
                }else{
                    imgCameraCapture.setImageResource(R.drawable.ic_baseline_camera_grey);
                }
                calendarRetro = Calendar.getInstance();
                Calendar compareRetro = (Calendar)curdateRetro.clone();
                compareRetro.add(Calendar.MILLISECOND, 500);
                if(calendarRetro.compareTo(compareRetro) > 0){
//                    Log.d("JAIDEN","Every second");
                    //upload images
                    retrofitMultiFileUpload();
                    imageFilePaths.clear();
                    calendarRetro2 = Calendar.getInstance();
                    Calendar compareRetro2 = (Calendar)curdateRetro2.clone();
                    compareRetro2.add(Calendar.SECOND, 2);
                    if(calendarRetro2.compareTo(compareRetro2)>0){
                        //delete files
                        Log.d("JAIDEN", "STARTING DELETE");
//                        for (String filePath: sentImages) {
//                            File delete_file = new File(filePath);
//                            if(delete_file.exists()){
//                                delete_file.delete();
//                                Log.d("JAIDEN", "DELETED FILE");
//                            }
//                        }
//                        sentImages.clear();
                        Log.d("JAIDEN", "ENDING DELETE");

                        calendarRetro3 = Calendar.getInstance();
                        Calendar compareRetro3 = (Calendar)curdateRetro3.clone();
                        compareRetro3.add(Calendar.SECOND, 10);
                        if(calendarRetro3.compareTo(compareRetro3)>0) {
                            retroIsDone = true;
                            File directory = new File(Environment.getExternalStorageDirectory()+"/Download/");
                            for (File fullFileDelete: Objects.requireNonNull(directory.listFiles())) {
                                if (!fullFileDelete.isDirectory()) {
                                    fullFileDelete.delete();
                                    Log.d("JAIDEN", "TOTAL FLUSH");
                                }
                            }
                            curdateRetro3 = Calendar.getInstance();
                        }

                        curdateRetro2 = Calendar.getInstance();
                    }
                    curdateRetro = Calendar.getInstance();
                }
            }

            @Override
            public void onMultipleFaceDetected() {

            }
        }));

    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);

        return b;
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }
    protected void screenshot() throws IOException {
        View view1 = getWindow().getDecorView().getRootView();
        view1.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view1.getDrawingCache());
        view1.setDrawingCacheEnabled(false);
        String fileName = Calendar.getInstance().getTime().toString().replaceAll(":", ".");

        String filePath = Environment.getExternalStorageDirectory()+"/Download/"+ fileName +".jpg";

//        File fileScreenshot = new File(filePath);
        FileOutputStream fileOutputStream = null;
        // TRY CATCH BLOC
        try {
            fileOutputStream = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
//            doFileUpload(filePath);
            imageFilePaths.add(filePath);
            Log.d("JAIDEN","Took Image: "+imageFilePaths.size());
        } catch (Exception e) {
            Log.d("JAIDEN", "Error: "+e.toString());
            e.printStackTrace();

        }
//        retrofitFileUpload(filePath);
    }
    public void clearFiles () {

    }
    public void retrofitMultiFileUpload () { // uploads all
        if(retroIsDone){
            retroIsDone = false;
            Log.d("JAIDEN", "UPLOAD CALLED");
            if (imageFilePaths.size() <=0) return;
            Log.d("JAIDEN", "UPLOAD PART 2");
            String baseUrl = "http://10.0.2.2:8000/";
//        baseUrl = "http://192.168.86.39:8000/";
            baseUrl = "http://192.168.86.59:8000/";

            ArrayList<MultipartBody.Part> images = new ArrayList<>();
            int i = 0;
            for (String filePath: imageFilePaths) {
                File file = new File(filePath);
                if(file.exists()){
                    MultipartBody.Part imagePart = MultipartBody.Part.createFormData("kiosk2-"+ String.valueOf((System.currentTimeMillis() / 1000)) + "-"+imageIndex, file.getName(), RequestBody.create(MediaType.parse("image/*"), file));
                    Log.d("JAIDEN", imageIndex+"");
                    images.add(imagePart);
                    previewImagePaths.add(filePath);
                    i++;
                    imageIndex++;
                }
            }
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create()) // gson is used to parse the response
                    .build();
            ImageApi imageApi = retrofit.create(ImageApi.class);
            MultipartBody.Part[] imagesArr = new MultipartBody.Part[images.size()];
            imagesArr = images.toArray(imagesArr);
            Log.d("JAIDEN", images.toString());
            Call<ResponseBody> call = imageApi.postImages(imagesArr);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d("JAIDEN", "RESPONSE ACCEPTED: "+ response.message());
                    if (!response.isSuccessful()) {
                        sentImages.addAll(previewImagePaths);
                        return;
                    }
                    tempDeleteFiles();
                    imageFilePaths.clear();
                    retroIsDone = true;
                    return;
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.d("JAIDEN", "FAILURE");
                    Log.d("JAIDEN", ""+t);
                    sentImages.addAll(previewImagePaths);
                    tempDeleteFiles();
                    imageFilePaths.clear();
                    retroIsDone = true;
                    return;
                }
            });
            ongoingCalls.add(call);
            Log.d("JAIDEN", "IsExecuted: "+call.isExecuted()+" "+(imageIndex-1));
        }
    }
    public void retrofitFileUpload (String path) {

        String baseUrl = "http://10.0.2.2:8000/";

//        String path = Environment.getExternalStorageDirectory().toString()+"/Pictures/Screenshots";
//        Log.d("JAIDEN", "Path: " + path);
//        File directory = new File(path);
//        File[] files = directory.listFiles();
//        Log.d("JAIDEN", "Size: "+ files.length);
//        for (int i = 0; i < files.length; i++)
//        {
//            Log.d("JAIDEN", "FileName:" + files[i].getName());
//        }
//        Log.d("JAIDEN", System.getProperty("user.dir"));
////        File testFile = new File("MainActivity.java");
////        Log.d("JAIDEN", "HI: "+testFile.getParent());
        File file = new File(path);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), RequestBody.create(MediaType.parse("image/*"), file));

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create()) // gson is used to parse the response
                .build();
        ImageApi imageApi = retrofit.create(ImageApi.class);
        Call<ResponseBody> call = imageApi.postImage(imagePart);
        Log.d("JAIDEN", "BEFORE");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d("JAIDEN", "RESPONSE ACCEPTED");
                if (!response.isSuccessful()) {
                    return;
                }
                return;
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("JAIDEN", "FAILURE");
                Log.d("JAIDEN", ""+t);
                return;
            }
        });
        Toast.makeText(this, "SEND REQUEST", Toast.LENGTH_SHORT).show();
    }

    public void doFileUpload(String path){
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        String pathToOurFile = path;
        String urlServer = "http://127.0.0.1:8000";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1*1024*1024;

        try
        {
            FileInputStream fileInputStream = new FileInputStream(new File(pathToOurFile) );

            URL url = new URL(urlServer);
            connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs &amp; Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);

            outputStream = new DataOutputStream( connection.getOutputStream() );
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + pathToOurFile +"\"" + lineEnd);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // Read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0)
            {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();
            Log.d("Message", serverResponseMessage);
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
        }
        catch (Exception ex)
        {
            //Exception handling
        }
    }

    @Override
    public void onResume() {
//        handler.postDelayed(runnable = new Runnable() {
//            public void run() {
//                handler.postDelayed(runnable, delay);
//            }
//        }, delay);
        super.onResume();
        Log.d("JAIDEN", "onResume");
        createCameraSource();
        startCameraSource();


    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }
    public void tempDeleteFiles () {
        for (String filePath: sentImages) {
            File delete_file = new File(filePath);
            if(delete_file.exists()){
                delete_file.delete();
                Log.d("JAIDEN", "DELETED FILE");
            }
        }
        sentImages.clear();
    }
}
