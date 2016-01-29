package es.smartidea.android.legalalerts.alertsBuilders;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;

import es.smartidea.android.legalalerts.MainActivity;
import es.smartidea.android.legalalerts.R;

public class AlertsNotificationBuilder {

    // Private empty constructor
    private AlertsNotificationBuilder() {}

    public static class Builder {

        private Context context;
        private NotificationCompat.Builder notification;
        // Notification ID
        private static final int ALERT_NOTIFICATION_ID = 0;


        public Builder(Context context){
            this.context = context;
        }

        public Builder setTitle(String title) {
            notification = new NotificationCompat.Builder(context);
            notification.setTicker(context.getResources().getString(R.string.app_name) + " - " + title)
                    .setContentTitle(title);
            return this;
        }

        public Builder setMessage(String message) {
            notification.setContentText(message);
            return this;
        }

        public Builder setVibrate(boolean vibrateON){
            if (vibrateON){
                notification.setVibrate(new long[]{0L, 500L, 250L, 500L});
            }
            return this;
        }

        public Builder setSound(String notificationSoundPath){
            notification.setSound(Uri.parse(notificationSoundPath));
            return this;
        }

        public void send() {
            notification.setSmallIcon(android.R.drawable.ic_popup_reminder);
            // Define notification´s associated intent action
            Intent intent = new Intent(context, MainActivity.class);
            // Put Fragment (int) identifier on "start_on_fragment" (where to start if app is not running)
            intent.putExtra("start_on_fragment", MainActivity.FRAGMENT_HISTORY);
            intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(ALERT_NOTIFICATION_ID, notification.build());
        }

    }
}
