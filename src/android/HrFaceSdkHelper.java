package cordova.plugin.face.recognize;

import android.graphics.Rect;
import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;

import java.util.ArrayList;
import java.util.List;

class HrFaceSdkHelper {
    private String appId;
    private String checkSDKKey;
    private AFD_FSDKEngine fdEngine;

    HrFaceSdkHelper( String appId, String checkSDKKey) {
        this.appId = appId;
        this.checkSDKKey = checkSDKKey;
        init();
    }

    private void init() {
        fdEngine = new AFD_FSDKEngine();
        //初始化人脸检测引擎，使用时请替换申请的 APPID 和 SDKKEY
        AFD_FSDKError err = fdEngine.AFD_FSDK_InitialFaceEngine(appId,checkSDKKey,
                AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 25);
        Log.d("com.arcsoft", "AFD_FSDK_InitialFaceEngine = " + err.getCode());
    }

    void destroy() {
        //销毁人脸检测引擎
        AFD_FSDKError err = fdEngine.AFD_FSDK_UninitialFaceEngine();
        Log.d("com.arcsoft", "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
    }

    List<AFD_FSDKFace> checkFace(byte[] data, int width, int height) {
        // 用来存放检测到的人脸信息列表
        List<AFD_FSDKFace> result = new ArrayList<>();
        //输入的 data 数据为 NV21 格式（如 Camera 里 NV21 格式的 preview 数据），其中 height 不能为奇数，人脸检测返回结果保存在 result。
        AFD_FSDKError err = fdEngine.AFD_FSDK_StillImageFaceDetection(data, width, height, AFD_FSDKEngine.CP_PAF_NV21, result);
        Log.e("ddd", String.valueOf(err.getCode()));
        Log.d("com.arcsoft", "AFD_FSDK_StillImageFaceDetection =" + err.getCode());
        Log.d("com.arcsoft", "Face=" + result.size()); for (AFD_FSDKFace face : result) {
            Log.d("com.arcsoft", "Face:" + face.toString());
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
