package es.smartidea.android.legalalerts.services.wakelock;

import android.content.Context;
import android.os.PowerManager;

import es.smartidea.android.legalalerts.utils.FileLogger;

/**
 * Public class containing a static reference to an WakeLock
 * that will be handled from other classes
 */
public class AlertsWakeLock {

    private static final String LOG_TAG = "AlertsWakelock";
    // Private static WakeLock reference
    private static PowerManager.WakeLock wakeLock;

    /**
     * Binds received WakeLock reference to inner static reference
     * and self handles acquiring the WakeLock itself, releasing previous wakelock if any
     *
     * @param context   received application context to get power service
     *                  and acquire WakeLock to bind it to inner reference
     */
    public static void setWakeLock(final Context context){
        // First release any possible wake locks
        doRelease();
        // Acquire WakeLock at first, and assign to its reference holder
        PowerManager pM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alerts_wakeLock");
        wakeLock.acquire();

        // Log to file for debugging
        FileLogger.logToExternalFile(LOG_TAG + " - Acquiring WakeLock...");
    }

    /**
     * Releases WakeLock sets reference to null (GC friendly)
     */
    public static void doRelease(){
        if (wakeLock != null && wakeLock.isHeld()) {
            // Log to file for debugging
            FileLogger.logToExternalFile(LOG_TAG + " - Releasing WakeLock...");

            wakeLock.release();
            //wakeLock = null;
        }
    }
}
