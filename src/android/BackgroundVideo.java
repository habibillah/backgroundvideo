package io.iclue.backgroundvideo;

import android.os.Environment;

import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BackgroundVideo extends CordovaPlugin {
    private static final String TAG = "BACKGROUND_VIDEO";
    private static final String ACTION_START_CAMERA = "startCamera";
    private static final String ACTION_START_RECORDING = "start";
    private static final String ACTION_STOP_RECORDING = "stop";
    private static final String FILE_EXTENSION = ".mp4";
    private static final int START_REQUEST_CODE = 0;

    private String FILE_PATH = "";
    private VideoOverlay videoOverlay;
    private CallbackContext callbackContext;
    private JSONArray requestArgs;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/";
        FILE_PATH = cordova.getActivity().getCacheDir().toString() + "/";
        // FILE_PATH = cordova.getActivity().getFilesDir().toString() + "/";
        //FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/";
    }


    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.requestArgs = args;

        try {
            Log.d(TAG, "ACTION: " + action);

            if (ACTION_START_CAMERA.equalsIgnoreCase(action)) {
                List<String> permissions = new ArrayList<String>();
                if (!cordova.hasPermission(android.Manifest.permission.CAMERA)) {
                    permissions.add(android.Manifest.permission.CAMERA);
                }
                if (permissions.size() > 0) {
                    cordova.requestPermissions(this, START_REQUEST_CODE, permissions.toArray(new String[0]));
                    return true;
                }

                StartCamera(this.requestArgs);
                return true;
            }

            if (ACTION_START_RECORDING.equalsIgnoreCase(action)) {

                List<String> permissions = new ArrayList<String>();
                if (!cordova.hasPermission(android.Manifest.permission.CAMERA)) {
                    permissions.add(android.Manifest.permission.CAMERA);
                }
                if (!cordova.hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
                    permissions.add(android.Manifest.permission.RECORD_AUDIO);
                }
                if (permissions.size() > 0) {
                    cordova.requestPermissions(this, START_REQUEST_CODE, permissions.toArray(new String[0]));
                    return true;
                }

                Start(this.requestArgs);
                return true;
            }

            if (ACTION_STOP_RECORDING.equalsIgnoreCase(action)) {
                Stop();
                return true;
            }

            callbackContext.error(TAG + ": INVALID ACTION");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "ERROR: " + e.getMessage(), e);
            callbackContext.error(TAG + ": " + e.getMessage());
        }

        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                callbackContext.error("Camera Permission Denied");
                return;
            }
        }

        if (requestCode == START_REQUEST_CODE) {
            Start(this.requestArgs);
        }
    }

    private void StartCamera(JSONArray args) throws JSONException {
        // params camera
        _ShowCamera(true, args.getString(0));
    }

    private void _ShowCamera(Boolean useCallback, String cameraFace) {
        if (videoOverlay == null) {
            videoOverlay = new VideoOverlay(cordova.getActivity()); //, getFilePath());

            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cordova.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    try {
                        // Get screen dimensions
                        DisplayMetrics displaymetrics = new DisplayMetrics();
                        cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

                        // NOTE: GT-I9300 testing required wrapping view in relative layout for setAlpha to work.
                        RelativeLayout containerView = new RelativeLayout(cordova.getActivity());
//                        containerView.addView(videoOverlay, new ViewGroup.LayoutParams(displaymetrics.widthPixels, displaymetrics.heightPixels));
//
//                        cordova.getActivity().addContentView(containerView, new ViewGroup.LayoutParams(displaymetrics.widthPixels, displaymetrics.heightPixels));

                        double previewBoxKoef;
                        double previewWrapperBoxKoefWidth;
                        double previewWrapperBoxKoefHeight;
                        if (cameraFace.equalsIgnoreCase("FRONT")) {
                            previewBoxKoef = 0.25;
                            previewWrapperBoxKoefWidth = 0.97;
                            previewWrapperBoxKoefHeight = 0.95;
                        } else {
                            previewBoxKoef = 1;
                            previewWrapperBoxKoefWidth = 1;
                            previewWrapperBoxKoefHeight = 1;
                        }

                        ViewGroup.LayoutParams l_params = new ViewGroup.LayoutParams((int)(displaymetrics.widthPixels * previewBoxKoef), (int)(displaymetrics.heightPixels * previewBoxKoef));
                        containerView.addView(videoOverlay, l_params);
                        containerView.setHorizontalGravity(Gravity.LEFT);
                        containerView.setVerticalGravity(Gravity.BOTTOM);
                        cordova.getActivity().addContentView(containerView, new ViewGroup.LayoutParams((int)(displaymetrics.widthPixels * previewWrapperBoxKoefWidth), (int)(displaymetrics.heightPixels * previewWrapperBoxKoefHeight)));

                        webView.getView().setBackgroundColor(0x00000000);
                        if (cameraFace.equalsIgnoreCase("FRONT")) {

                        } else {
                            ((ViewGroup)webView.getView()).bringToFront();
                        }
                        if (useCallback) {
                            callbackContext.success();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during preview create", e);
                        callbackContext.error(TAG + ": " + e.getMessage());
                    }
                }
            });

            videoOverlay.setCameraFacing(cameraFace);
        }
    }

    private void Start(JSONArray args) throws JSONException {
        // params fileStorage, filename, camera, quality
        final String filename = args.getString(1);
        final String cameraFace = args.getString(2);

        if (videoOverlay == null) {
            _ShowCamera(false, cameraFace);
        }

        videoOverlay.setCameraFacing(cameraFace);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    videoOverlay.Start(getFilePath(filename));
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void Stop() throws JSONException {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (videoOverlay != null) {
                    try {
                        String filepath = videoOverlay.Stop();
                        videoOverlay = null;
                        callbackContext.success(filepath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        callbackContext.error(e.getMessage());
                    }
                }
            }
        });
    }

    private String getFilePath(String filename) {
        // Add number suffix if file exists
        int i = 1;
        String fileName = filename;
        while (new File(FILE_PATH + fileName + FILE_EXTENSION).exists()) {
            fileName = filename + '_' + i;
            i++;
        }
        return FILE_PATH + fileName + FILE_EXTENSION;
    }

    //Plugin Method Overrides
    @Override
    public void onPause(boolean multitasking) {
        if (videoOverlay != null) {
            try {
                this.Stop();
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error(e.getMessage());
            }
        }
        super.onPause(multitasking);
    }

    @Override
    public void onDestroy() {
        try {
            this.Stop();
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        super.onDestroy();
    }
}
