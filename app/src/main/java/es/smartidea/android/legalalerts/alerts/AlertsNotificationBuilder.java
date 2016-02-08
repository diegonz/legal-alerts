package es.smartidea.android.legalalerts.alerts;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;

import es.smartidea.android.legalalerts.MainActivity;
import es.smartidea.android.legalalerts.R;

public class AlertsNotificationBuilder {

    // Private empty constructor to avoid instantiation
    private AlertsNotificationBuilder() {}

    public static class Builder {
        private Context context;
        private NotificationCompat.Builder notification;
        private static final int ALERT_NOTIFICATION_ID = 0;

        /**
         * Builder constructor class that binds received context and returns a Builder object
         *
         * @param context   Context of application to bind
         */
        public Builder(final Context context){
            this.context = context;
        }

        /**
         * Sets notification title and returns a Builder object
         *
         * @param title String representing main title of notification
         * @return  AlertsNotificationBuilder object
         */
        public Builder setTitle(final String title) {
            notification = new NotificationCompat.Builder(context);
            notification.setTicker(context.getResources().getString(R.string.app_name) + " - " + title)
                    .setContentTitle(title);
            return this;
        }

        /**
         * Sets notification message and returns a Builder object
         *
         * @param message   String representing main notification´s message
         * @return  AlertsNotificationBuilder object
         */
        public Builder setMessage(final String message) {
            notification.setContentText(message);
            return this;
        }

        /**
         * Sets vibration pattern if boolean flag received vibrateON is TRUE
         * and returns a Builder object
         *
         * @param vibrateON boolean flag indicating if vibrate pattern has to be added
         * @return  AlertsNotificationBuilder object
         */
        public Builder setVibrate(final boolean vibrateON){
            if (vibrateON) notification.setVibrate(new long[]{0L, 500L, 250L, 500L});
            //else notification.setVibrate(new long[]{0L, 0L, 0L, 0L});
            return this;
        }

        /**
         * Sets notification sound parsing to URI given String as path to the sound
         * and returns a Builder object
         *
         * @param notificationSoundPath String representing path (URI)
         *                              to desired notification sound
         * @return AlertsNotificationBuilder object
         */
        public Builder setSound(final String notificationSoundPath){
            notification.setSound(Uri.parse(notificationSoundPath));
            return this;
        }

        /**
         * Sends/shows the notification setting up notification icon and
         * according to already set up parameters.
         * Also does the releasing of internal references
         * (context & notification) after using it calling to releaseReferences
         */
        public void send() {
            notification.setSmallIcon(android.R.drawable.ic_popup_reminder);
            Intent intent = new Intent(context, MainActivity.class);
            // Put Fragment (int) identifier on "start_on_fragment" (fragment to show OnNewIntent())
            intent.putExtra("start_on_fragment", MainActivity.FRAGMENT_HISTORY);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(ALERT_NOTIFICATION_ID, notification.build());

            // Release references after Notification sent
            releaseReferences();
        }

        /**
         * Sets to null all inner references
         */
        private void releaseReferences() {
            // Release already used references
            if (this.context != null) this.context = null;
            if (this.notification != null) this.notification = null;
        }
    }
}
