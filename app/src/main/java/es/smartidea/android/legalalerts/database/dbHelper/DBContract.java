package es.smartidea.android.legalalerts.database.dbHelper;

import android.provider.BaseColumns;

/*
 * DBContract class
 * database structure handler class
 * Defines DB structure (Tables, Columns, etc...)
 * Contains publicly accessible Strings describing DB
 * Changes on DB structure are managed from here, applying on whole app.
 */
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
        public static final String _ID = "_id";
        public static final String COL_ALERT_NAME = "alert_name";
        public static final String COL_ALERT_SEARCH_NOT_LITERAL = "alert_search_not_literal";
    }

    public static abstract class History implements BaseColumns {
        public static final String TABLE_NAME = "history_table";
        public static final String _ID = "_id";
        public static final String COL_HISTORY_RELATED_ALERT_NAME = "history_related_alert_name";
        public static final String COL_HISTORY_DOCUMENT_NAME = "history_document_name";
        public static final String COL_HISTORY_DOCUMENT_URL = "history_document_url";
    }
}
