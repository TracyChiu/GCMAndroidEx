package idv.david.gcmandroidex;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * IntentService接受到傳來的intent後就執行onHandleIntent方法
 * 當方法結束後也將此Service停止
 */

public class GCMIntentService extends IntentService {
    private final static int NOTIFICATION_ID = 100;
    private final static String TAG = "GCMIntentService";

    public GCMIntentService() {
        super("This is GCMIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // 從GCMBroadcastReceiver帶過來的intent
        String messageType = gcm.getMessageType(intent);
        // 將GCM發出的訊息做type過濾，我們只需要讓使用者收到正常且正確的訊息內容
        if (!bundle.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + bundle.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + bundle.toString());
                // 若取得是GCM正常的Message type，就從bundle裡面取出訊息內容
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                String msg = null;
                try {
                    // 從server取得編碼的文字訊息在這邊做解碼動作
                    msg = URLDecoder.decode(intent.getExtras().getString("message"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                sendNotification(msg);
                Log.i(TAG, "Received: " + msg);
            }
        }
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_gcm)
                .setContentTitle("GCM Notification").setContentText(msg)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
