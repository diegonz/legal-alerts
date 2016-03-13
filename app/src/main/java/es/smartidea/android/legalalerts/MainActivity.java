package es.smartidea.android.legalalerts;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import es.smartidea.android.legalalerts.database.DBHelper;
import es.smartidea.android.legalalerts.receivers.AlarmReceiver;
import es.smartidea.android.legalalerts.database.DBContentProvider;
import es.smartidea.android.legalalerts.database.DBContract;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener, DialogInterface.OnDismissListener {

    private final static String WHERE_NAME_EQUALS = DBContract.Alerts.COL_ALERT_NAME + '=';
    private final static String DIALOG_TAG = "dialog_legal_alerts";
    private final static String RUNNING_FRAGMENT_STRING = "running_fragment";
    private String fabHint;
    public final static int FRAGMENT_ALERTS_ID = 0;
    public final static int FRAGMENT_HISTORY_ID = 1;
    private static int runningFragment = -1;

    @Bind(R.id.nav_view) NavigationView navigationView;
    @Bind(R.id.drawer_layout) DrawerLayout drawer;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fab) FloatingActionButton fab;

    @OnClick(R.id.fab) void fabClick(View view) {
        switch (runningFragment) {
            case FRAGMENT_ALERTS_ID:
                new LegalAlertDialog().show(getSupportFragmentManager(), DIALOG_TAG);
                break;
            case FRAGMENT_HISTORY_ID:
                // Delete all items
                showSnackBar(view, "Deleted " + deleteHistory(this, null, null) + " item(s).");
                break;
        }
    }
    @OnLongClick(R.id.fab) boolean fabLongClick(View view){
        Toast.makeText(this, fabHint, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        int targetFragment;
        if (savedInstanceState == null) {
            targetFragment = getIntent().getIntExtra("start_on_fragment", FRAGMENT_ALERTS_ID);
            sendBroadcast(new Intent(this, AlarmReceiver.class));
        } else {
            targetFragment = savedInstanceState.getInt(RUNNING_FRAGMENT_STRING, FRAGMENT_ALERTS_ID);
        }
        replaceFragment(targetFragment);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateDrawer();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("start_on_fragment")) {
            replaceFragment(intent.getIntExtra("start_on_fragment", FRAGMENT_ALERTS_ID));
        }
        updateDrawer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(RUNNING_FRAGMENT_STRING, runningFragment);
    }

    @Override
    public void onBackPressed() {
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        final int LAUNCH_DIALOG_ALERT = 1, START_SETTINGS_ACTIVITY = 2;
        int afterSelectionTask = 0;
        switch (item.getItemId()){
            case R.id.nav_alerts:
                replaceFragment(FRAGMENT_ALERTS_ID);
                break;
            case R.id.nav_add_alert:
                afterSelectionTask = LAUNCH_DIALOG_ALERT;
                break;
            case R.id.nav_history:
                replaceFragment(FRAGMENT_HISTORY_ID);
                break;
            case R.id.nav_settings:
                afterSelectionTask = START_SETTINGS_ACTIVITY;
                break;
            case R.id.nav_share:
            replaceFragment(FRAGMENT_ALERTS_ID);
                break;
            case R.id.nav_info:
            replaceFragment(FRAGMENT_ALERTS_ID);
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        switch (afterSelectionTask) {
            case LAUNCH_DIALOG_ALERT:
                new LegalAlertDialog().show(getSupportFragmentManager(), DIALOG_TAG);
                break;
            case START_SETTINGS_ACTIVITY:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        updateDrawer();
    }

    /**
     * Activity method to insert new Alerts into Database according to given alertName
     * and boolean flag isLiteralSearch
     *
     * @param context   Context to get ContentResolver
     * @param alertName String representing the search term to be stored
     * @param isLiteralSearch boolean flag indicating if search
     *                        term has literal search set to TRUE
     * @return int indicating DB ID, returns minus one (-1) if there was an
     * error or same search term already existed into DB
     */
    public static int insertNewAlert(Context context, String alertName, boolean isLiteralSearch){
        ContentValues values = new ContentValues();
        values.put(DBContract.Alerts.COL_ALERT_NAME, alertName);
        int literalSearchIntValue = (isLiteralSearch) ? 0 : 1;
        values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
        Uri resultID = context.getContentResolver().insert(DBContentProvider.ALERTS_URI, values);
        return resultID != null ? Integer.parseInt(resultID.getLastPathSegment()) : -1;
    }

    /**
     * Activity method to update Alerts into Database according to given alertName
     *
     * @param context   Context to get ContentResolver
     * @param oldName   String representing previous alertName to be updated
     * @param newName   String representing new alertName to update
     * @param isLiteralSearch   boolean flag indicating if search
     *                          term has literal search set to TRUE
     * @return int representing updating success, 1 if updated ok, -1 if no update was produced
     */
    public static int updateAlert(Context context, String oldName, String newName, boolean isLiteralSearch){
        ContentValues values = new ContentValues();
        values.put(DBContract.Alerts.COL_ALERT_NAME, newName);
        int literalSearchIntValue = (isLiteralSearch) ? 0 : 1;
        values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
        return context.getContentResolver().update(DBContentProvider.ALERTS_URI, values,
                WHERE_NAME_EQUALS + '\'' + oldName + '\'', null) < 1 ? -1 : 1;
    }

    /**
     * Activity method to delete Alerts into Database according to given alertName
     *
     * @param context   Context to get ContentResolver
     * @param alertName String representing alertName to be deleted
     * @return int representing number of deleted items
     */
    public static int deleteAlert(Context context, String alertName){
        return context.getContentResolver().delete(DBContentProvider.ALERTS_URI,
                DBContract.Alerts.COL_ALERT_NAME + "='" + alertName + '\'', null);
    }

    /**
     * Delete items from history table according to given parameters or deletes all items
     * if <var>documentName</var> and <var>alertName</var> where passed as NULL
     *
     * @param context       <type>Context</type> reference to get ContentResolver
     * @param documentName  <annotation>@Nullable</annotation> <type>String</type> corresponding
     *                      to history item to delete
     * @param alertName     @Nullable <type>String</type> corresponding to history item to delete
     * @return  <type>int</type> indicating number of items deleted or -1 if no action taken.
     */
    public static int deleteHistory(Context context, @Nullable String documentName, @Nullable String alertName){
        if (documentName != null && alertName != null){
            //noinspection StringConcatenationMissingWhitespace
            String WHERE = DBHelper.SPACE_OPEN_BRACKET +
                    DBContract.History.COL_HISTORY_DOCUMENT_NAME + "='" + documentName + '\'' +
                    DBHelper.CLOSE_BRACKET_SPACE + " AND " + DBHelper.SPACE_OPEN_BRACKET +
                    DBContract.History.COL_HISTORY_RELATED_ALERT_NAME + "='" + alertName + '\'' +
                    DBHelper.CLOSE_BRACKET_SPACE;
            return context.getContentResolver().delete(DBContentProvider.HISTORY_URI, WHERE, null);
        } else if (documentName == null && alertName == null) {
            return context.getContentResolver().delete(DBContentProvider.HISTORY_URI, null, null);
        }
        return -1;
    }

    /**
     * Replaces Fragment according to given fragmentID on main fragment placeholder
     *
     * @param fragmentID : Numeric ID of objective fragment
     **/
    public void replaceFragment(int fragmentID) {
        if (runningFragment != fragmentID) {
            // TODO refactor implementation
            try {
                switch (fragmentID) {
                    case FRAGMENT_ALERTS_ID:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.fragmentMainPlaceholder, AlertsFragment.class.newInstance())
                                .commit();
                        break;
                    case FRAGMENT_HISTORY_ID:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                .replace(R.id.fragmentMainPlaceholder, HistoryFragment.class.newInstance())
                                .commit();
                        break;
                    default:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.fragmentMainPlaceholder, AlertsFragment.class.newInstance())
                                .commit();
                        break;
                }
                runningFragment = fragmentID;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        updateDrawer();
        updateFabButton();
    }

    /**
     * Sets title and checked navigation drawer item according to given fragment ID
     *
     * @param fragmentID int containing fragment ID
     */
    public void setDrawerCheckedItemAndTitle(int fragmentID) {
        switch (fragmentID) {
            case FRAGMENT_ALERTS_ID:
                navigationView.setCheckedItem(R.id.nav_alerts);
                setTitle(getResources().getString(R.string.nav_alerts));
                break;
            case FRAGMENT_HISTORY_ID:
                navigationView.setCheckedItem(R.id.nav_history);
                setTitle(getResources().getString(R.string.nav_history));
                break;
        }
    }

    /**
     * Sets title and checked navigation drawer item according to currently running fragment
     */
    public void updateDrawer() {
        setDrawerCheckedItemAndTitle(runningFragment);
    }

    /**
     * Sets FAB icon and content description according current fragment
     * and its known int resource IDs
     */
    private void updateFabButton(){
        int iconID, contentDescription;
        switch (runningFragment){
            case FRAGMENT_ALERTS_ID:
                iconID = R.drawable.ic_add_new;
                contentDescription = R.string.fab_description_alerts;
                break;
            case FRAGMENT_HISTORY_ID:
                iconID = R.drawable.ic_delete_sweep_white;
                contentDescription = R.string.fab_description_history;
                break;
            default:
                iconID = R.drawable.ic_add_new;
                contentDescription = R.string.fab_description_alerts;
                break;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setImageDrawable(getResources().getDrawable(iconID, getTheme()));
        } else {
            //noinspection deprecation
            fab.setImageDrawable(getResources().getDrawable(iconID));
        }
        fabHint = getString(contentDescription);
        fab.setContentDescription(fabHint);
    }

    /**
     * Shows a SnackBar with given text
     *
     * @param view  Parent view to show the SnackBar on.
     * @param message   String containing message to show
     */
    public static void showSnackBar(@NonNull View view, @NonNull String message){
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }
}