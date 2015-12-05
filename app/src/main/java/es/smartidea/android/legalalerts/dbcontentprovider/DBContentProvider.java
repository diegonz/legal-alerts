package es.smartidea.android.legalalerts.dbcontentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

import es.smartidea.android.legalalerts.dbhelper.DBContract;
import es.smartidea.android.legalalerts.dbhelper.DBHelper;

import android.support.annotation.NonNull;

/**
 * DBContentProvider class abstracts database access layer on a ContentProvider
 *
 * -> @SuppressWarnings("ConstantConditions") annotation ignores null context warnings,
 * as in theory we avoid them cause its always called from MainActivity.
 * Can also be checked inline with "if (getContext() != null)" or similar sentence.
 */

@SuppressWarnings("ConstantConditions")
public class DBContentProvider extends ContentProvider {
    // DBHelper declaration
    private DBHelper dbHelper;
    //UriMatcher values
    private static final String AUTHORITY = "es.smartidea.legalalerts.dbcontentprovider";
    // Access URI to Alerts table
    private static final int ALERTS = 10;
    private static final String ALERTS_PATH = "alerts_table";
    public static final Uri ALERTS_URI = Uri.parse("content://" + AUTHORITY + "/" + ALERTS_PATH);
    // Access URI to History table
    private static final int HISTORY = 20;
    private static final String HISTORY_PATH = "history_table";
    public static final Uri HISTORY_URI = Uri.parse("content://" + AUTHORITY + "/" + HISTORY_PATH);

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, ALERTS_PATH, ALERTS);
        sUriMatcher.addURI(AUTHORITY, HISTORY_PATH, HISTORY);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    // query() method queries DB and returns a Cursor to result set
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Saves obtained URI type (DB tables)
        int uriType = sUriMatcher.match(uri);

        // Use SQLiteQueryBuilder instead of query()
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check queried columns are the correct ones
        checkColumns(uriType, projection);

        switch (uriType) {
            case ALERTS:
                queryBuilder.setTables(DBContract.Alerts.TABLE_NAME);
                break;
            case HISTORY:
                queryBuilder.setTables(DBContract.History.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        // Send change notifications to potential listeners (CursorLoader)
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    // insert() handles an insert to DB, notifies changes to ContentResolver and returns the URI
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = sUriMatcher.match(uri);
        String path;
        long id;
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (uriType) {
            case ALERTS:
                path = ALERTS_PATH;
                id = db.insert(DBContract.Alerts.TABLE_NAME, null, values);
                Log.d("DB", "Inserted into Alerts table, ID: " + id);
                break;
            case HISTORY:
                path = HISTORY_PATH;
                id = db.insert(DBContract.History.TABLE_NAME, null, values);
                Log.d("DB", "Inserted into History table, ID: " + id);
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(path + "/" + id);
    }

    // delete() handles deletions on DB, notifies changes and returns number of deleted rows
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int uriType = sUriMatcher.match(uri);
        int rowsDeleted;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (uriType) {
            case ALERTS:
                rowsDeleted = db.delete(DBContract.Alerts.TABLE_NAME, selection, selectionArgs);
                Log.d("DB", rowsDeleted + "row(s) deleted from Alerts table!");
                break;
            case HISTORY:
                rowsDeleted = db.delete(DBContract.History.TABLE_NAME, selection, selectionArgs);
                Log.d("DB", rowsDeleted + "row(s) deleted from History table!");
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    // update() handles updates on DB records, notifies changes and returns number of updated cells
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sUriMatcher.match(uri);
        int rowsUpdated;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (uriType) {
            case ALERTS:
                rowsUpdated = db.update(DBContract.Alerts.TABLE_NAME, values, selection, selectionArgs);
                Log.d("DB", rowsUpdated + "row(s) updated from Alerts table!");
                break;
            case HISTORY:
                rowsUpdated = db.update(DBContract.History.TABLE_NAME, values, selection, selectionArgs);
                Log.d("DB", rowsUpdated + "row(s) updated from History table!");
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    // checkColumns() checks if received projection´s columns are valid
    public void checkColumns(int uriType, String[] projection) {

        String[] availableC;
        HashSet<String> requested;
        HashSet<String> available;

        if (projection != null) {

            switch (uriType) {
                case ALERTS:
                    availableC = new String[]{
                            DBContract.Alerts._ID,
                            DBContract.Alerts.COL_ALERT_NAME,
                            DBContract.Alerts.COL_ALERT_SEARCH_LITERAL
                    };
                    requested = new HashSet<>(Arrays.asList(projection));
                    available = new HashSet<>(Arrays.asList(availableC));
                    // Checks if all columns are available
                    if (!available.containsAll(requested)) {
                        throw new IllegalArgumentException("Invalid columns on projection");
                    }
                    break;
                case HISTORY:
                    availableC = new String[]{
                            DBContract.History._ID,
                            DBContract.History.COL_HISTORY_RELATED_ALERT_NAME,
                            DBContract.History.COL_HISTORY_DOCUMENT_NAME,
                            DBContract.History.COL_HISTORY_DOCUMENT_URL
                    };
                    requested = new HashSet<>(Arrays.asList(projection));
                    available = new HashSet<>(Arrays.asList(availableC));
                    // Checks if all columns are available
                    if (!available.containsAll(requested)) {
                        throw new IllegalArgumentException("Invalid columns on projection");
                    }
                    break;
            }
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}
