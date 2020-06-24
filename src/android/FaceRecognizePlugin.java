package cordova.plugin.face.recognize;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FaceRecognizePlugin extends CordovaPlugin {
    private static final String TAG = FaceRecognizePlugin.class.getSimpleName();

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
        frSDKkey = preferences.getString("fr_sdk_key","");
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
        Intent intent = new Intent();
        Context context = webView.getContext();
        intent.setClass(context, FacePreviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("app_id", appId);
        bundle.putString("fd_sdk_key", fdSDKkey);
        bundle.putString("fr_sdk_key", frSDKkey);
        
         try{
            boolean isCollect = message.getBoolean("isCollect");
            if (!isCollect) {
                bundle.putString("unique_id", message.getString("bizId"));
                bundle.putString("query_url", message.getString("queryUrl"));
            }
            bundle.putBoolean("isCollect", message.getBoolean("isCollect"));
            Log.e("message", message.toString());
        } catch (JSONException e) {
            Log.e("previewTag", e.getMessage());
        }

        intent.putExtras(bundle);
        context.startActivity(intent);
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
