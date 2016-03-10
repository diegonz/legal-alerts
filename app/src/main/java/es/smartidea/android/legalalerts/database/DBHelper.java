package es.smartidea.android.legalalerts.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * DBHelper class, does CRUD stuff on DB
 * It contains and handles query and methods to CRUD and upgrade/downgrade DB
 */

@SuppressWarnings("StringConcatenationMissingWhitespace")
public class DBHelper extends SQLiteOpenHelper {

    // String query "tokens" with preceding space
    public final static String TEXT_TYPE = " TEXT", INT_TYPE = " INT", NOT_NULL = " NOT NULL";
    public final static String SPACE_OPEN_BRACKET = " (", SPACE_CLOSE_BRACKET_SEMICOLON = " );";
    public final static String CLOSE_BRACKET_SPACE = ") ";
    private final static String ON_CONFLICT_IGNORE = " ON CONFLICT IGNORE";
    private final static String DEFAULT_0 = " DEFAULT 0";
    private final static String UNIQUE = "UNIQUE ", COMMA_SEP = ", ";
    private final static String INT_PRIMARY_KEY = " INTEGER PRIMARY KEY, ";

    /*
    *  Start ALERTS table
    * */

    public final static String SQL_CREATE_ALERTS_TABLE =
        "CREATE TABLE " + DBContract.Alerts.TABLE_NAME + SPACE_OPEN_BRACKET +
            DBContract.Alerts._ID + INT_PRIMARY_KEY +
            DBContract.Alerts.COL_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL + INT_TYPE + DEFAULT_0 + COMMA_SEP +
            UNIQUE + SPACE_OPEN_BRACKET + DBContract.Alerts.COL_ALERT_NAME + CLOSE_BRACKET_SPACE +
                ON_CONFLICT_IGNORE + SPACE_CLOSE_BRACKET_SEMICOLON;

    public final static String SQL_DELETE_ALERTS_TABLE =
        "DROP TABLE IF EXISTS " + DBContract.Alerts.TABLE_NAME + ';';

    /*
    *  End ALERTS table
    * */

    /*
    *  Start HISTORY table
    * */

    public final static String SQL_CREATE_HISTORY_TABLE =
        "CREATE TABLE " + DBContract.History.TABLE_NAME + SPACE_OPEN_BRACKET +
            DBContract.History._ID + INT_PRIMARY_KEY +
            DBContract.History.COL_HISTORY_RELATED_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.History.COL_HISTORY_DOCUMENT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            DBContract.History.COL_HISTORY_DOCUMENT_URL + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            UNIQUE + SPACE_OPEN_BRACKET + DBContract.History.COL_HISTORY_RELATED_ALERT_NAME +
                COMMA_SEP + DBContract.History.COL_HISTORY_DOCUMENT_NAME + CLOSE_BRACKET_SPACE +
                ON_CONFLICT_IGNORE + SPACE_CLOSE_BRACKET_SEMICOLON;

    public final static String SQL_DELETE_HISTORY_TABLE =
        "DROP TABLE IF EXISTS " + DBContract.History.TABLE_NAME + ';';

    /*
    * End HISTORY table
    * */

    // Public constructor
    public DBHelper(Context context) {
        super(context, DBContract.DATABASE_NAME, null, DBContract.DATABASE_VERSION);
    }

    // onCreate, onUpgrade and onDowngrade implementations

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
        Log.d("DB", "Database dump complete!");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Call onUpgrade to dump database
        onUpgrade(db, oldVersion, newVersion);
    }

}
