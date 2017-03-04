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
class DBHelper extends SQLiteOpenHelper {

    // String query "tokens" with preceding space
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INT";
    private static final String NOT_NULL = " NOT NULL";
    private static final String SPACE_OPEN_BRACKET = " (";
    private static final String SPACE_CLOSE_BRACKET_SEMICOLON = " );";
    private static final String CLOSE_BRACKET_SPACE = ") ";
    private static final String ON_CONFLICT_IGNORE = " ON CONFLICT IGNORE";
    private static final String DEFAULT_0 = " DEFAULT 0";
    private static final String UNIQUE = "UNIQUE ";
    private static final String COMMA_SEP = ", ";
    private static final String INT_PRIMARY_KEY = " INTEGER PRIMARY KEY, ";

    /*
    *  Start ALERTS table
    * */

    private static final String SQL_CREATE_ALERTS_TABLE =
            "CREATE TABLE " + DBContract.Alerts.TABLE_NAME + SPACE_OPEN_BRACKET +
                    DBContract.Alerts.ID + INT_PRIMARY_KEY +
                    DBContract.Alerts.COL_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL + INT_TYPE + DEFAULT_0 + COMMA_SEP +
                    UNIQUE + SPACE_OPEN_BRACKET + DBContract.Alerts.COL_ALERT_NAME + CLOSE_BRACKET_SPACE +
                    ON_CONFLICT_IGNORE + SPACE_CLOSE_BRACKET_SEMICOLON;

    private static final String SQL_DELETE_ALERTS_TABLE =
            "DROP TABLE IF EXISTS " + DBContract.Alerts.TABLE_NAME + ';';

    /*
    *  End ALERTS table
    * */

    /*
    *  Start HISTORY table
    * */

    private static final String SQL_CREATE_HISTORY_TABLE =
            "CREATE TABLE " + DBContract.History.TABLE_NAME + SPACE_OPEN_BRACKET +
                    DBContract.History.ID + INT_PRIMARY_KEY +
                    DBContract.History.COL_HISTORY_RELATED_ALERT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    DBContract.History.COL_HISTORY_DOCUMENT_NAME + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    DBContract.History.COL_HISTORY_DOCUMENT_URL + TEXT_TYPE + NOT_NULL + COMMA_SEP +
                    UNIQUE + SPACE_OPEN_BRACKET + DBContract.History.COL_HISTORY_RELATED_ALERT_NAME +
                    COMMA_SEP + DBContract.History.COL_HISTORY_DOCUMENT_NAME + CLOSE_BRACKET_SPACE +
                    ON_CONFLICT_IGNORE + SPACE_CLOSE_BRACKET_SEMICOLON;

    private static final String SQL_DELETE_HISTORY_TABLE =
            "DROP TABLE IF EXISTS " + DBContract.History.TABLE_NAME + ';';

    /*
    * End HISTORY table
    * */

    // Public constructor
    DBHelper(Context context) {
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
