package cordova.plugin.face.recognize;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.security.rp.RPSDK;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceRecognizePlugin extends CordovaPlugin {
    private static final String TAG = FaceRecognizePlugin.class.getSimpleName();
    private static String QueryUrl = "";

    private String appId;
    private String fdSDKkey;
    private String frSDKkey;

    private BroadcastReceiver receiver;
    private CallbackContext backendCallbackContext;

    public FaceRecognizePlugin() {}

    @Override
    protected void pluginInitialize() {
        appId = preferences.getString("face_app_id", "");
        fdSDKkey = preferences.getString("fd_sdk_key", "");
        QueryUrl = preferences.getString("query_url","");
        // 初始化阿里云活体检测SDK
        RPSDK.initialize(webView.getContext().getApplicationContext());
        super.pluginInitialize();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject message = args.getJSONObject(0);
        Log.d(TAG, action);
        if ("start".equals(action)) {
            registryEvent(callbackContext);
            startPreview(message);
            return true;
        }
        return false;
    }

    private void registryEvent(CallbackContext callbackContext) {
        if (this.backendCallbackContext != null) {
            removeBatteryListener();
        }
        this.backendCallbackContext = callbackContext;

        // We need to listen to power events to update battery status
        IntentFilter intentFilter = new IntentFilter("android.corodva.face.check.Action");
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateMessageInfo(intent);
                }
            };
            webView.getContext().registerReceiver(receiver, intentFilter);
        }

        // Don't return any result now, since status results will be sent when events come in from broadcast receiver
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    /**
     * Stop battery receiver.
     */
    public void onDestroy() {
        removeBatteryListener();
    }

    /**
     * Stop battery receiver.
     */
    public void onReset() {
        removeBatteryListener();
    }

    /**
     * Stop the battery receiver and set it to null.
     */
    private void removeBatteryListener() {
        if (receiver != null) {
            try {
                webView.getContext().unregisterReceiver(receiver);
                receiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage(), e);
            }
        }
    }

    private void startPreview(JSONObject message) {
        try{
            boolean isCollect = message.getBoolean("isCollect");
            // 采集
            if (isCollect) {
                Bundle bundle = new Bundle();
                bundle.putString("app_id", appId);
                bundle.putString("fd_sdk_key", fdSDKkey);
                bundle.putString("fr_sdk_key", frSDKkey);
                bundle.putString("unique_id", message.getString("uniqueId"));
                bundle.putBoolean("isCollect", message.getBoolean("isCollect") );
                Intent intent = new Intent();
                Context context = webView.getContext();
                intent.setClass(context, FacePreviewActivity.class);
                intent.putExtras(bundle);
                context.startActivity(intent);
            } else {
                // 验证 阿里云活体人脸验证
                String token = message.getString("token");
                String bizId = message.getString("bizId");
                Log.e(TAG, token);
                RPSDK.startVerifyByNative(token, webView.getContext().getApplicationContext(), new RPSDK.RPCompletedListener() {
                    @Override
                    public void onAuditResult(RPSDK.AUDIT audit, String s) {
                        // 远程查询
                        Log.e(TAG, audit.name());
                        JSONObject jb = new JSONObject();
                        try {
                            jb.put("biz_id", bizId);
                            OkHttpClient client = new OkHttpClient();
                            MediaType type = MediaType.parse("application/json; charset=utf-8");
                            final Request request = new Request.Builder()
                                    .url(QueryUrl)
                                    .post(RequestBody.create(type, jb.toString())).build();
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    setFail();
                                }
                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    Log.e(TAG, response.toString());
                                    if (response.code() == 200) {
                                        try {
                                            String rs = new String(response.body().bytes());
                                            JSONObject jb = new JSONObject(rs);
                                            boolean success = jb.getBoolean("success");
                                            if (success) {
                                                setSuccess();
                                                return;
                                            }
                                        } catch (JSONException e){
                                            Log.e(TAG, e.getMessage());
                                        }
                                    }
                                    setFail();
                                }
                            });
                        } catch ( JSONException e ){
                            setFail();
                        }
                    }
                });
            }
        } catch (JSONException e) {
            setFail();
        }
    }

    /**
     * Creates a JSONObject with the current battery information
     *
     * @param intent the current battery information
     * @return a JSONObject containing the battery status information
     */
    private JSONObject getMessageInfo(Intent intent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("status", intent.getBooleanExtra("result", false));
            obj.put("image", intent.getStringExtra("data"));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return obj;
    }

    /**
     * Updates the JavaScript side whenever the battery changes
     *
     * @param batteryIntent the current battery information
     */
    private void updateMessageInfo(Intent batteryIntent) {
        sendUpdate(this.getMessageInfo(batteryIntent));
    }

    private void setSuccess() {
        try {
            JSONObject json = new JSONObject();
            json.put("status", true);
            json.put("image", "");
            sendUpdate(json);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setFail() {
        try {
            JSONObject json = new JSONObject();
            json.put("status", false);
            json.put("image", "");
            sendUpdate(json);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     */
    private void sendUpdate(JSONObject info) {
        if (this.backendCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(false);
            this.backendCallbackContext.sendPluginResult(result);
        }
    }
}
