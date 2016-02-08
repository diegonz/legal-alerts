package es.smartidea.android.legalalerts.alerts;

import android.os.PowerManager;

/**
 * Public class containing a static reference to an WakeLock
 * that will be handled from other classes
 */
public class AlertsWakeLock {

    // Private static WakeLock reference
    private static PowerManager.WakeLock wakeLock;

    /**
     * Binds received WakeLock reference to inner static reference
     * and self handles acquiring the WakeLock itself, releasing previous wakelock if any
     *
     * @param receivedWakeLock received WakeLock to bind to inner reference
     */
    public static void setWakeLock(final PowerManager.WakeLock receivedWakeLock){
        //noinspection VariableNotUsedInsideIf
        if (wakeLock != null) doRelease();
        wakeLock = receivedWakeLock;
        wakeLock.acquire();
    }

    /**
     * Releases WakeLock sets reference to null (GC friendly)
     */
    public static void doRelease(){
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
