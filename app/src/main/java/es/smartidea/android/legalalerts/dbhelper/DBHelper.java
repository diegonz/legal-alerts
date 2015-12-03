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

    // Create and delete table sentences
    private static final String TEXT_TYPE = " TEXT";
//    private static final String INT_TYPE = " INT";
//    private static final String COMMA_SEP = ",";
    public static final String SQL_CREATE_ALERTS =
            "CREATE TABLE " + DBContract.Alerts.TABLE_NAME + " (" +
                    DBContract.Alerts._ID + " INTEGER PRIMARY KEY," +
                    DBContract.Alerts.COL_ALERT_NAME + TEXT_TYPE + " );";

    public static final String SQL_DELETE_ALERTS =
            "DROP TABLE IF EXISTS " + DBContract.Alerts.TABLE_NAME + ";";

    public DBHelper(Context context) {
        super(context, DBContract.DATABASE_NAME, null, DBContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v("si", "Creating Database...");
        db.execSQL(SQL_CREATE_ALERTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Method to handle DB upgrade
        db.execSQL(SQL_DELETE_ALERTS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
