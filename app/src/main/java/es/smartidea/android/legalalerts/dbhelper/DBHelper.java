package es.smartidea.android.legalalerts.dbhelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * DBHelper class, does CRUD stuff on DB
 * It contains and handles query and methods to CRUD and upgrade/downgrade DB
 */

public class DBHelper extends SQLiteOpenHelper {

    // String query "tokens"
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INT";
    private static final String NOT_NULL = " NOT NULL";
    private static final String DEFAULT_0 = " DEFAULT 0";
    private static final String COMMA_SEP = ",";

    // Create and delete table sentences

    /* Start ALERTS table */
    public static final String SQL_CREATE_ALERTS =
            "CREATE TABLE " + DBContract.Alerts.TABLE_NAME + " (" +
                    DBContract.Alerts._ID + " INTEGER PRIMARY KEY," +
                    DBContract.Alerts.COL_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    DBContract.Alerts.COL_ALERT_SEARCH_LITERAL + INT_TYPE + DEFAULT_0 + " );";

    public static final String SQL_DELETE_ALERTS =
            "DROP TABLE IF EXISTS " + DBContract.Alerts.TABLE_NAME + ";";
    /* End ALERTS table */

    /* Start HISTORY table */
    public static final String SQL_CREATE_HISTORY =
            "CREATE TABLE " + DBContract.History.TABLE_NAME + " (" +
            DBContract.History._ID + " INTEGER PRIMARY KEY," +
            DBContract.History.COL_HISTORY_RELATED_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.History.COL_HISTORY_DOCUMENT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.History.COL_HISTORY_DOCUMENT_URL + TEXT_TYPE + NOT_NULL + " );";

    public static final String SQL_DELETE_HISTORY =
            "DROP TABLE IF EXISTS " + DBContract.History.TABLE_NAME + ";";
    /* End HISTORY table */

    public DBHelper(Context context) {
        super(context, DBContract.DATABASE_NAME, null, DBContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DB", "Creating Database...");
        db.execSQL(SQL_CREATE_ALERTS);
        db.execSQL(SQL_CREATE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Method to handle DB upgrade
        Log.d("DB", "Dumping Database on upgrade...");
        db.execSQL(SQL_DELETE_ALERTS);
        db.execSQL(SQL_DELETE_HISTORY);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
