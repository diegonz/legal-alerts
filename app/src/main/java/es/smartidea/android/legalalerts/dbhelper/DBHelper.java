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
    public static final String SQL_CREATE_ALERTS_TABLE =
            "CREATE TABLE " + DBContract.Alerts.TABLE_NAME + " (" +
                    DBContract.Alerts._ID + " INTEGER PRIMARY KEY," +
                    DBContract.Alerts.COL_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL + INT_TYPE + DEFAULT_0 + " );";

    public static final String SQL_DELETE_ALERTS_TABLE =
            "DROP TABLE IF EXISTS " + DBContract.Alerts.TABLE_NAME + ";";
    /* End ALERTS table */

    /* Start HISTORY table */
    public static final String SQL_CREATE_HISTORY_TABLE =
            "CREATE TABLE " + DBContract.History.TABLE_NAME + " (" +
            DBContract.History._ID + " INTEGER PRIMARY KEY," +
            DBContract.History.COL_HISTORY_RELATED_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.History.COL_HISTORY_DOCUMENT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.History.COL_HISTORY_DOCUMENT_URL + TEXT_TYPE + NOT_NULL + " );";

    public static final String SQL_DELETE_HISTORY_TABLE =
            "DROP TABLE IF EXISTS " + DBContract.History.TABLE_NAME + ";";
    /* End HISTORY table */

    public DBHelper(Context context) {
        super(context, DBContract.DATABASE_NAME, null, DBContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DB", "Creating Database tables...");
        db.execSQL(SQL_CREATE_ALERTS_TABLE);
        db.execSQL(SQL_CREATE_HISTORY_TABLE);
        Log.d("DB", "Created Database tables!");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Method to handle DB upgrade
        Log.d("DB", "Deleting Database tables on upgrade...");
        db.execSQL(SQL_DELETE_ALERTS_TABLE);
        db.execSQL(SQL_DELETE_HISTORY_TABLE);
        Log.d("DB", "Deleted!");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
