package cordova.plugin.face.recognize;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.arcsoft.facedetection.AFD_FSDKFace;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FacePreviewActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = FacePreviewActivity.class.getSimpleName();
    private SurfaceHolder faceHolder;
    SurfaceView faceView;
    private Camera mCamera;
    private String appId;
    private String checkSDKkey;
    final String compareUrl = "http://XXXXXX/push/face/compare";
    private HrFaceSdkHelper checkHelper;
    private String uniqueId;
    private TextView showText;
    private Button options;
    private boolean isCollect = false;
    private boolean startCollect = false;
    private HrFaceSdkHelper.StoreFace storeFace; // 检测到的数据
    private boolean result = false;
    private int previewWidth;
    private int previewHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            Intent intent = getIntent();
            appId = intent.getStringExtra("app_id");
            checkSDKkey = intent.getStringExtra("fd_sdk_key");
        } catch (NullPointerException e){
            Log.e(TAG,"获取sdk授权数据错误：" + e.getMessage());
        }

        uniqueId = getIntent().getStringExtra("unique_id");
        isCollect = getIntent().getBooleanExtra("isCollect", false);
        createView();
        super.onCreate(savedInstanceState);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //  用户未彻底拒绝授予权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }
        }
        start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    start();
                } else {
                   // todo
                }
            }
        }
    }

    private void createView() {
        setContentView( getApplication().getResources().getIdentifier("preview", "layout", getApplication().getPackageName()));
        faceView = findViewById(getApplication().getResources().getIdentifier("facePreview", "id", getApplication().getPackageName()));
        faceView.setBackgroundColor(Color.TRANSPARENT);
        faceHolder = faceView.getHolder();
        faceHolder.addCallback(this);
        showText = findViewById(getApplication().getResources().getIdentifier("show_text", "id", getApplication().getPackageName()));
        options = findViewById(getApplication().getResources().getIdentifier("options", "id", getApplication().getPackageName()));
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCollect = true;
            }
        });

        options.setText("身份校验");
        if (isCollect) {
            options.setText("开始采集");
        }
        // 返回
        Button returnBtn = findViewById(getApplication().getResources().getIdentifier("returnBack", "id", getApplication().getPackageName()));
        returnBtn.setBackgroundColor(Color.TRANSPARENT);
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                releaseCameraAndPreview();
                finish();
            }
        });
    }

    private Camera safeCameraOpen(int id) {
        try {
            Camera camera = Camera.open(id);// 旋转90度
            camera.setDisplayOrientation(90);
            camera.setPreviewCallback(this);

            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> picList= parameters.getSupportedPictureSizes();
            int width = 0, height = 0;
            for (Camera.Size size : picList) {
                if (width <= 0) {
                    width = size.width;
                    height = size.height;
                } else if (size.width < width) {
                    width = size.width;
                    height = size.height;
                }
            }
            previewHeight =  height;
            previewWidth = width;
            List<String> focusModes = parameters.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            } else {
                Log.e(TAG, "phone not surppot auto focus");
            }
            camera.cancelAutoFocus();
            parameters.setPreviewSize(previewWidth,previewHeight);
            camera.setParameters(parameters);
            return camera;
        } catch (Exception e) {
            Log.e(getString(getApplication().getResources().getIdentifier("app_name", "string", getApplication().getPackageName())), "failed to open Camera");
            e.printStackTrace();
        }

        return null;
    }

    private int getFrontCameraIndex() {
        int frontIndex =-1;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for(int cameraIndex = 0; cameraIndex<cameraCount; cameraIndex++){
            Camera.getCameraInfo(cameraIndex, info);
            Log.d(TAG, String.valueOf(info.facing));
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                frontIndex = cameraIndex;
                Log.d(TAG, info.toString());
                break;
            }
        }
        return frontIndex;
    }

    public void setCamera(Camera camera) {
        if (mCamera == camera) {
            return;
        }

        stopPreviewAndFreeCamera();

        mCamera = camera;

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(faceHolder);
                mCamera.startPreview();
                mCamera.autoFocus(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            Log.d(TAG, "preview");
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseCameraAndPreview() {
        setCamera(null);
    }

    @Override
    protected void onDestroy() {
        releaseCameraAndPreview();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        releaseCameraAndPreview();
        String pic = "";
        if (storeFace != null) {
            pic = saveFace(storeFace, isCollect);
        }

        Intent intent = new Intent("android.corodva.face.check.Action");
        intent.putExtra("result", result);
        intent.putExtra("data", pic);
        sendBroadcast(intent);
        Log.e(TAG, "on stop");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        requestPermission();
        super.onRestart();
    }

    private void start() {
        Camera camera = this.safeCameraOpen(getFrontCameraIndex());
        this.setCamera(camera);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        requestPermission();
        try {
            checkHelper = new HrFaceSdkHelper(appId, checkSDKkey);
        } catch (Exception e){
            Log.e(TAG, "init checkHelper error: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCameraAndPreview();
        checkHelper.destroy();
    }

    private String getLocalAuthFile() {
        return getExternalCacheDir() + "/" + "face_"+uniqueId+".im";
    }

    private void compareFace(HrFaceSdkHelper.StoreFace storeFace) {
        Timer timer = new Timer();
        try {
            FileInputStream in = new FileInputStream(getLocalAuthFile());
            byte[] buf = new byte[1024];
            int length = 0;
            StringBuilder buffer = new StringBuilder();
            while((length = in.read(buf)) != -1){
                buffer.append(new String(buf,0,length));
            }
            //最后记得，关闭流
            in.close();
            String authData =  buffer.toString();
            String[] parts = authData.split(";", 4);
            String basePicData = parts[0];

            try {
                Log.e(TAG, basePicData);
                JSONObject compare = new JSONObject();
                String newFace = saveFace(storeFace, false);
                compare.put("type", 1); // 0,1 区分比较的内容 0 传递url 1传递内容这里传递内容
                compare.put("content_1", basePicData);
                compare.put("content_2", newFace);
                OkHttpClient client = new OkHttpClient();
                MediaType type = MediaType.parse("application/json; charset=utf-8");
                final Request request = new Request.Builder()
                        .url(compareUrl)
                        .post(RequestBody.create(type, compare.toString())).build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "compare face request fail : " + e.getMessage());
                        showText.setText("请求验证失败！请稍后重试");
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1000);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.e(TAG, response.toString());
                        if (response.code() == 200) {
                            try {
                                String rs = new String(response.body().bytes());
                                JSONObject jb = new JSONObject(rs);
                                result = jb.getBoolean("success");
                                showText.setText( result ? "身份验证通过" : "身份验证不通过");
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 1000);
                            } catch (JSONException e){
                                Log.e(TAG, e.getMessage());
                            }
                        } else {
                            showText.setText("请求验证失败！请稍后重试");
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, 1000);
                        }
                    }
                });
            } catch (JSONException e) {
                showText.setText("请求验证失败！请稍后重试");
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1000);
                Log.e(TAG, e.getMessage());
            }
        } catch (IOException e){
            showText.setText("本地采集失效！请联系管理员");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    finish();
                }
            }, 1000);
            Log.e(TAG, e.getMessage());
        }
    }

    private String saveFace(HrFaceSdkHelper.StoreFace storeFace, boolean save) {
        // byte to bitmap
        byte[] bytes = storeFace.getBytes();
        int width = storeFace.getWidth();
        int height = storeFace.getHeight();
        Rect rect = storeFace.getRect();

        Bitmap maps = CommonUtils.nv21ToBitmap(getApplicationContext(), bytes, width, height);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //该方法用来压缩图片，第一个参数为图片格式，第二个参数为截取图片的保留率，如当前为90，则保留之前图片90%的区域
            maps.compress(Bitmap.CompressFormat.JPEG,100,outputStream );
            //得到图片的String
            String pic = Base64.encodeToString( outputStream.toByteArray(), Base64.NO_WRAP);
            // 本地存储采集数据
            if (save) {
                String storeString = pic + ";" + width + ":" + height + ";" + rect.top + ":" + rect.right + ":" + rect.left + ":" + rect.bottom + ";" + storeFace.getAFR_FOC();

                FileOutputStream fos = new FileOutputStream(getLocalAuthFile());
                fos.write(storeString.getBytes());
                fos.close();
            }
            return pic;
        } catch (IOException e) {
            Log.e(TAG, "bitmap2base64 error: " + e.getMessage());
        }
        return "";
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (startCollect) {
            List<AFD_FSDKFace> results = checkHelper.checkFace(bytes, previewWidth, previewHeight);
            if (results.size() > 0) {
                startCollect = false;
                releaseCameraAndPreview();
                storeFace = new HrFaceSdkHelper.StoreFace();
                storeFace.setBytes(bytes);
                storeFace.setWidth(previewWidth);
                storeFace.setHeight(previewHeight);
                storeFace.setRect(results.get(0).getRect());
                storeFace.setAFR_FOC(results.get(0).getDegree());

                if (isCollect) {
                    showText.setText("采集完成...");
                    options.setVisibility(View.INVISIBLE);
                    result = true;
                    finish();
                } else {
                    showText.setText("正在验证请稍等……");
                    compareFace(storeFace);
                }
                Log.d(TAG, "检测到人脸");
            }
        }
    }
}
