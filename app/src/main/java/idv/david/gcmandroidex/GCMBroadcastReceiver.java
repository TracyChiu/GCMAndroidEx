package idv.david.gcmandroidex;


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * WakefulBroadcastReceiver接受到一個喚醒事件後即交由Service處理
 * 並能保證在Service執行期間，該裝置不會進入到休眠狀態而中斷
 * 需注意一定要在AndroidManifest.xml加入WAKE_LOCK permission
 */

public class GCMBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, GCMIntentService.class);
        startWakefulService(context, intent);
    }
}
