package idv.david.gcmandroidex;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private final static String PREF_NAME = "gcm_pref";
    private final static String KEY_NAME_REG_ID = "regId";
    private final static String KEY_NAME_APP_VERSION = "appVersion";
    private final static String URL = "http://114.34.3.186:7080/GCMServerEx/RegisterServlet";
    // 模擬器使用此URL
//    private final static String URL = "http://10.0.2.2:8081/GCMServerEx/RegisterServlet";
    //Google Play Services專用的請求代碼
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    //從Google API Console上取得的專案編號即為SenderID
    private final static String SENDER_ID = "408514132291";
    private GoogleCloudMessaging gcm;
    private TextView tvRegId;
    private ProgressDialog progressDialog;
    private String regId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvRegId = (TextView) findViewById(R.id.tvRegId);
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegId();
            if (regId.isEmpty())
                new RegisterTask().execute();
        }
    }

    // 由手動方式重新向Server註冊
    public void onSendToServerClick(View view) {
        new RegisterTask().execute();
    }

    /**
     * 原官方GCM文件使用的 GooglePlayServicesUtil多數方法已經deprecated
     * 改使用建議的GoogleApiAvailability類別與其方法
     * <p/>
     * 檢查該裝置是否有Google Play Services APK
     * 若是沒有就跳出提示訊息要求使用者安裝
     * Dialog已由Google實作完畢，我們只需呼叫方法即可
     *
     * @return checked result
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                //當使用者點擊對話視窗的確認，系統即自動轉到Google Play Store
                googleApiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                String errMsg = "This device is not supported by Google Play Services.";
                Log.e(TAG, errMsg);
                Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * 從偏好設定檔裡找到已儲存的Registration ID
     *
     * @return Registration ID
     */
    private String getRegId() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String regId = preferences.getString(KEY_NAME_REG_ID, "");
        if (regId == null || regId.isEmpty()) {
            Log.i(TAG, "Registration id not found.");
            return "";
        }

        int appVersionInPrefs = preferences.getInt(KEY_NAME_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (appVersionInPrefs != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return regId;
    }

    /**
     * 將得到的Registration ID與當前的app版本編號存入偏好設定檔裡
     *
     * @param regId Registration ID
     */
    private void storeRegId(String regId) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int appVersion = getAppVersion();
        Log.i(TAG, "Saving regId on app version " + appVersion);
        preferences.edit()
                .putString(KEY_NAME_REG_ID, regId)
                .putInt(KEY_NAME_APP_VERSION, appVersion)
                .apply();
    }

    /**
     * 取得當前app的版本號碼
     *
     * @return Current app version code
     */

    private int getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException nfe) {
            throw new RuntimeException("Could not get package name: " + nfe);
        }
    }

    /**
     *  1. 送出SENDER_ID給Server
     *  2. 取得由Server回傳的Registration ID
     */

    private class RegisterTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Processing...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            String msg;
            try {
                if (gcm == null)
                    gcm = GoogleCloudMessaging.getInstance(MainActivity.this);
                regId = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID = " + regId;
                Log.d(TAG, msg);
                sendRegIdToServer();
                storeRegId(regId);
            } catch (IOException ie) {
                msg = "Error :" + ie.getMessage();
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            progressDialog.cancel();
            tvRegId.setText(msg + "\n");
        }

        private void sendRegIdToServer() {
            DataOutputStream dos = null;
            HttpURLConnection con = null;
            try {
                URL url = new URL(URL);
                con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setUseCaches(false);
                con.connect();

                dos = new DataOutputStream(con.getOutputStream());
                String content = "regId=" + URLEncoder.encode(regId, "UTF-8");
                dos.writeBytes(content);
                dos.flush();

                int statusCode = con.getResponseCode();
                if (statusCode == 200) {
                    Log.e(TAG, "send regId to server");
                } else {
                    throw new Exception();
                }

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }

                if (con != null) {
                    con.disconnect();
                }
            }
        }


    }

}
