package es.smartidea.android.legalalerts.dbhelper;

import android.provider.BaseColumns;

/**
 * DBContract class
 * Defines DB structure (Tables, Colmns, etc...)
 * Contains publicly accessible Strings describing DB
 * Changes on DB structure are managed from here, applying on whole app.
 */

// DB structure handler class
public final class DBContract {
    // void constructor to avoid instantiation
    public DBContract() {
    }

    // Database name and version
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Alerts.db";

    // Corresponding subclass per table
    // Subclass to define "alerts_table" table
    public static abstract class Alerts implements BaseColumns {
        public static final String TABLE_NAME = "alerts_table";
        public static final String _ID = "_id";                         // _id of Alert
        public static final String COL_ALERT_NAME = "alert_name";       // Name of Alert
    }
}
