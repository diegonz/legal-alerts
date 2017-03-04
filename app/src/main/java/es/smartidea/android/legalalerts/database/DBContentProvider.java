package es.smartidea.android.legalalerts.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

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
    private static final String AUTHORITY = "es.smartidea.legalalerts.dbContentProvider";
    // Access URI to Alerts table
    private static final int ALERTS_URI_INT = 10;
    private static final String ALERTS_PATH = "alerts_table";
    public static final Uri ALERTS_URI = Uri.parse("content://" + AUTHORITY + '/' + ALERTS_PATH);
    // Access URI to History table
    private static final int HISTORY_URI_INT = 20;
    private static final String HISTORY_PATH = "history_table";
    private static final String WRONG_URI_MESSAGE = "ERROR - Wrong URI: ";
    public static final Uri HISTORY_URI = Uri.parse("content://" + AUTHORITY + '/' + HISTORY_PATH);

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private DBHelper dbHelper;

    static {
        sUriMatcher.addURI(AUTHORITY, ALERTS_PATH, ALERTS_URI_INT);
        sUriMatcher.addURI(AUTHORITY, HISTORY_PATH, HISTORY_URI_INT);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = sUriMatcher.match(uri);
        String path;
        long id;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (uriType) {
            case ALERTS_URI_INT:
                path = ALERTS_PATH;
                id = db.insert(DBContract.Alerts.TABLE_NAME, null, values);
                break;

            case HISTORY_URI_INT:
                path = HISTORY_PATH;
                id = db.insert(DBContract.History.TABLE_NAME, null, values);
                break;

            default:
                throw new IllegalArgumentException(String.format("%s%s", WRONG_URI_MESSAGE, uri));
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(path + '/' + id);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        int uriType = sUriMatcher.match(uri);
        // Use SQLiteQueryBuilder instead of query()
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        checkColumns(uriType, projection);
        switch (uriType) {
            case ALERTS_URI_INT:
                queryBuilder.setTables(DBContract.Alerts.TABLE_NAME);
                break;
            case HISTORY_URI_INT:
                queryBuilder.setTables(DBContract.History.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException(String.format("%s%s", WRONG_URI_MESSAGE, uri));
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor =
                queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sUriMatcher.match(uri);
        int rowsUpdated;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (uriType) {
            case ALERTS_URI_INT:
                rowsUpdated =
                        db.update(DBContract.Alerts.TABLE_NAME, values, selection, selectionArgs);
                break;
            case HISTORY_URI_INT:
                rowsUpdated =
                        db.update(DBContract.History.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(String.format("%s%s", WRONG_URI_MESSAGE, uri));
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        int uriType = sUriMatcher.match(uri);
        int rowsDeleted;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (uriType) {
            case ALERTS_URI_INT:
                rowsDeleted = db.delete(DBContract.Alerts.TABLE_NAME, selection, selectionArgs);
                break;
            case HISTORY_URI_INT:
                rowsDeleted = db.delete(DBContract.History.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(String.format("%s%s", WRONG_URI_MESSAGE, uri));
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    /**
     * Checks if received projection´s columns are valid
     * throwing an IllegalArgumentException if not all fields are contained
     *
     * @param uriType   int representing the URI of each DB table
     * @param projection    received VarArgs String[] containing DB columns
     *                      to check against DB structure
     */
    public static void checkColumns(int uriType, @NonNull String... projection) {

        HashSet<String> requested = new HashSet<>(Arrays.asList(projection));
        HashSet<String> available;
        switch (uriType) {
            case ALERTS_URI_INT:
                available = new HashSet<>(Arrays.asList(DBContract.ALERTS_PROJECTION));
                break;
            case HISTORY_URI_INT:
                available = new HashSet<>(Arrays.asList(DBContract.HISTORY_PROJECTION));
                break;
            default:
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(), "%s%d", WRONG_URI_MESSAGE, uriType));
        }
        if (!available.containsAll(requested))
            throw new IllegalArgumentException("Invalid columns on projection");
    }

    @Override
    public String getType(@NonNull Uri uri) {
        //noinspection ReturnOfNull
        return null;
    }
}
