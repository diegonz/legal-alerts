package es.smartidea.android.legalalerts.database;

import android.provider.BaseColumns;

/*
 * DBContract class Defines DB structure (Tables, Columns, etc...)
 *
 * Database structure definer class using android BaseColumns
 * Contains publicly accessible constant Strings referring DB names
 * Changes on DB versions and structure are managed from here.
 */
public final class DBContract {

    // Database name and version
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Alerts.db";

    // Projection String[] containing all fields of alerts_table
    public static final String[] ALERTS_PROJECTION = {
            Alerts.ID,
            Alerts.COL_ALERT_NAME,
            Alerts.COL_ALERT_SEARCH_NOT_LITERAL
    };

    // Projection String[] containing all fields of history_table
    public static final String[] HISTORY_PROJECTION = {
            History.ID,
            History.COL_HISTORY_RELATED_ALERT_NAME,
            History.COL_HISTORY_DOCUMENT_NAME,
            History.COL_HISTORY_DOCUMENT_URL
    };

    // void constructor to avoid instantiation
    public DBContract() {
    }

    // Subclass to define "alerts_table" table
    public abstract static class Alerts implements BaseColumns {
        public static final String TABLE_NAME = "alerts_table";
        public static final String ID = "_id";
        public static final String COL_ALERT_NAME = "alert_name";
        public static final String COL_ALERT_SEARCH_NOT_LITERAL = "alert_search_not_literal";
    }

    // Subclass to define "history_table" table
    public abstract static class History implements BaseColumns {
        public static final String TABLE_NAME = "history_table";
        public static final String ID = "_id";
        public static final String COL_HISTORY_RELATED_ALERT_NAME = "history_related_alert_name";
        public static final String COL_HISTORY_DOCUMENT_NAME = "history_document_name";
        public static final String COL_HISTORY_DOCUMENT_URL = "history_document_url";
    }
}
