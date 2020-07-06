package cordova.plugin.face.recognize;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;

import java.util.ArrayList;
import java.util.List;

class HrFaceSdkHelper {
    private String appId;
    private String checkSDKKey;
    private FaceEngine faceEngine;
    private static final String TAG = HrFaceSdkHelper.class.getSimpleName();

    HrFaceSdkHelper( String appId, String checkSDKKey) {
        this.appId = appId;
        this.checkSDKKey = checkSDKKey;
    }

    public int init(Context context) {
        //初始化人脸检测引擎，使用时请替换申请的 APPID 和 SDKKEY
        int code = FaceEngine.activeOnline(context, appId, checkSDKKey);
        if(code == ErrorInfo.MOK){
            Log.i(TAG, "activeOnline success");
        }else if(code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED){
            Log.i(TAG, "already activated");
        }else{
            Log.i(TAG, "activeOnline failed, code is : " + code);
            return code;
        }

        int scale = 16; // video 16 image 32
        int maxFaceNum = 1;
        // 如下的组合，初始化的功能包含：人脸检测、人脸识别、RGB活体检测、年龄、性别、人脸3D角度
        int initMask = FaceEngine.ASF_FACE_DETECT;

        faceEngine = new FaceEngine();
        code = faceEngine.init(context, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_0_ONLY, scale, maxFaceNum, initMask);
        if (code != ErrorInfo.MOK) {
            Log.e(TAG, "init failed, code is : " + code);
        } else {
            Log.i(TAG, "init success");
        }

        return code;
    }

    void destroy() {
        //销毁人脸检测引擎
        if (faceEngine != null) {
            int code = faceEngine.unInit();
            Log.d("com.arcsoft", "AFD_FSDK_UninitialFaceEngine =" + code);
        }
    }

    List<FaceInfo> checkFace(byte[] data, int width, int height) {
        // 用来存放检测到的人脸信息列表
        List<FaceInfo> result = new ArrayList<>();
        //输入的 data 数据为 NV21 格式（如 Camera 里 NV21 格式的 preview 数据），其中 height 不能为奇数，人脸检测返回结果保存在 result。
        try{
            int code = faceEngine.detectFaces(data, width, height, FaceEngine.CP_PAF_NV21, result);
            if (code == ErrorInfo.MOK && result.size() > 0) {
                Log.i(TAG, "detectFaces, face num is : "+ result.size() );
            } else {
                Log.i(TAG, "no face detected, code is : " + code);
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        return result;
    }

    static class StoreFace {
        private byte[] bytes;
        private int width;
        private int height;
        private Rect rect;
        private int AFR_FOC;

        StoreFace() {}

        void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        void setWidth(int width) {
            this.width = width;
        }

        void setHeight(int height) {
            this.height = height;
        }

        void setRect(Rect rect) {
            this.rect = rect;
        }

        void setAFR_FOC(int AFR_FOC) {
            this.AFR_FOC = AFR_FOC;
        }

        byte[] getBytes() {
            return bytes;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }

        Rect getRect() {
            return rect;
        }

        int getAFR_FOC() {
            return AFR_FOC;
        }
    }
}
