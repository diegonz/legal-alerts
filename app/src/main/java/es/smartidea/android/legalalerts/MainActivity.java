package es.smartidea.android.legalalerts;

import android.content.ContentValues;
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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.smartidea.android.legalalerts.broadcastReceivers.AlertsAlarmReceiver;
import es.smartidea.android.legalalerts.database.dbContentProvider.DBContentProvider;
import es.smartidea.android.legalalerts.database.dbHelper.DBContract;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // URI of Alerts table into DB
    private static final Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    private static final Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    private static final String WHERE_NAME_EQUALS = DBContract.Alerts.COL_ALERT_NAME + '=';

    // Integer Fragment identifiers
    public static final int FRAGMENT_ALERTS = 0;
    public static final int FRAGMENT_HISTORY = 1;
    private int runningFragment = -1;   // initialized to -1, forcing first replacement and update.
    private static final String DIALOG_TAG = "dialog_legal_alerts";
    // Running fragment string
    private static final String RUNNING_FRAGMENT_STRING = "running_fragment";
    // ButterKnife bindings
    @Bind(R.id.nav_view) NavigationView navigationView;
    @Bind(R.id.drawer_layout) DrawerLayout drawer;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fab) FloatingActionButton fab;
    @OnClick(R.id.fab)
    void fabClickListener(View view) {
        switch (runningFragment) {
            case FRAGMENT_ALERTS:
                new CustomAlertDialogFragment().show(getSupportFragmentManager(), DIALOG_TAG);
                break;
            case FRAGMENT_HISTORY:
                // Delete all items
                showSnackBar(view, "Deleted " + deleteHistory(null) + " item(s).");
                break;
        }
    }

    /*
    * LIFECYCLE
    * */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Bind ButterKnife to activity
        ButterKnife.bind(this);
        // Set actionbar
        setSupportActionBar(toolbar);
        // Set Drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        // Set NavigationView listener
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            // Check/start new alarm
            sendBroadcast(new Intent(this, AlertsAlarmReceiver.class));
            // Starting from scratch, getIntExtra from intent to replace
            // corresponding fragment, defaulting to FRAGMENT_ALERTS
            replaceFragment(getIntent().getIntExtra("start_on_fragment", FRAGMENT_ALERTS));
        } else {
            // Replace fragment according to intent extras or savedInstanceState
            // If starting from scratch it defaults to FRAGMENT_ALERTS
            replaceFragment(savedInstanceState.getInt(RUNNING_FRAGMENT_STRING, FRAGMENT_ALERTS));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Check intent extras and start fragment replacing
        if (intent.hasExtra("start_on_fragment")) {
            replaceFragment(intent.getIntExtra("start_on_fragment", FRAGMENT_ALERTS));
        }
        setDrawerCheckedItemAndTitle(runningFragment);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the user's current running fragment
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_settings) {
            // Start Settings activity
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        final int START_DIALOG_ALERT = 1;
        final int START_SETTINGS_ACTIVITY = 2;
        int afterSelectionTask = 0;

        switch (id){
            case R.id.nav_alerts:
                replaceFragment(FRAGMENT_ALERTS);
                break;
            case R.id.nav_add_alert:
                afterSelectionTask = START_DIALOG_ALERT;
                break;
            case R.id.nav_history:
                replaceFragment(FRAGMENT_HISTORY);
                break;
            case R.id.nav_settings:
                afterSelectionTask = START_SETTINGS_ACTIVITY;
                break;
            case R.id.nav_share:
//            replaceFragment(FRAGMENT_ALERTS);
                break;
            case R.id.nav_info:
//            replaceFragment(FRAGMENT_ALERTS);
                break;
        }

        // Launch afterSelectionTask if there is any
        switch (afterSelectionTask){
            case START_DIALOG_ALERT:
                // Launch Alerts dialog
                new CustomAlertDialogFragment().show(getSupportFragmentManager(), DIALOG_TAG);
                break;
            case START_SETTINGS_ACTIVITY:
                // Launch Settings Activity
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*
    * LOCAL METHODS
    * */

    // DB

    /**
     * Activity method to insert new Alerts into Database
     *
     * @param alertName String representing the search term to be stored
     * @param isLiteralSearch boolean flag indicating if search
     *                        term has literal search set to TRUE
     * @return int indicating DB ID, returns minus one (-1) if there was an
     * error or same search term already existed into DB
     */
    public int insertNewAlert(final String alertName, final boolean isLiteralSearch){
        ContentValues values = new ContentValues();
        values.put(DBContract.Alerts.COL_ALERT_NAME, alertName);
        // Get REVERSE toggle button state by casting a boolean with ternary operator expression.
        // If checked returns 0, if not returns 1
        int literalSearchIntValue = (isLiteralSearch) ? 0 : 1;
        values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
        Uri resultID = getContentResolver().insert(ALERTS_URI, values);
        if (resultID != null) {
            // Return last item of Uri (ID)
            return Integer.parseInt(resultID.getLastPathSegment());
        }
        // If not inserted return -1
        return -1;
    }

    /**
     * Activity method to update Alerts into Database
     *
     * @param oldName       String representing previous alertName to be updated
     * @param newName       String representing new alertName to update
     * @param isLiteralSearch   boolean flag indicating if search
     *                          term has literal search set to TRUE
     * @return int representing updating success, 1 if updated ok, -1 if no update was produced
     */
    public int updateAlert(final String oldName, final String newName, final boolean isLiteralSearch){
        ContentValues values = new ContentValues();
        values.put(DBContract.Alerts.COL_ALERT_NAME, newName);
        // Get REVERSE toggle button state by casting a boolean with ternary operator expression.
        // If checked returns 0, if not returns 1
        int literalSearchIntValue = (isLiteralSearch) ? 0 : 1;
        values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
        // Set WHERE clause between single quotes to avoid being
        // identified by SQLite as a table +info: http://stackoverflow.com/a/13173792/3799840
        return getContentResolver()
                .update(ALERTS_URI, values, WHERE_NAME_EQUALS + '\'' + oldName + '\'', null) < 1
                ? -1
                : 1;
    }

    /**
     * Delete items from history table according to given parameters
     *
     * @param historyItem    @Nullable String representing history item to delete
     *                       if NULL received deleted all entries in the table
     * @return  int indicating number of items deleted.
     */
    public int deleteHistory(@Nullable final String historyItem){
        // Deletes item according to received String, if null received deletes all records
        return getContentResolver().delete(HISTORY_URI, historyItem, null);
    }

    // UI

    /**
     * replaceFragment (final int fragmentID)
     * <p>
     * Replaces Fragment given by fragmentID on main fragment placeholder
     *
     * @param fragmentID : Numeric ID of objective fragment
     **/
    public void replaceFragment(final int fragmentID) {

        // Check if runningFragment is the same received
        if (runningFragment != fragmentID) {
            try {
                switch (fragmentID) {
                    case FRAGMENT_ALERTS:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(
                                        R.id.fragmentMainPlaceholder,
                                        AlertsFragment.class.newInstance())
                                .commit();
                        // Set FAB icon after replacing
                        setFabIcon(android.R.drawable.ic_input_add);
                        break;
                    case FRAGMENT_HISTORY:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                .replace(R.id.fragmentMainPlaceholder,
                                        HistoryFragment.class.newInstance())
                                .commit();
                        // Set FAB icon after replacing
                        setFabIcon(android.R.drawable.ic_menu_delete);
                        break;
                    default:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.fragmentMainPlaceholder,
                                        AlertsFragment.class.newInstance())
                                .commit();
                        // Set FAB icon after replacing
                        setFabIcon(android.R.drawable.ic_input_add);
                        break;
                }
                // Change running fragment id
                runningFragment = fragmentID;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        setDrawerCheckedItemAndTitle(runningFragment);
    }

    /**
     * Sets title and checked navigation drawer item according to given fragment ID
     *
     * @param fragmentID int containing fragment ID
     */
    private void setDrawerCheckedItemAndTitle(final int fragmentID) {
        // Select item on drawer
        switch (fragmentID) {
            case FRAGMENT_ALERTS:
                navigationView.setCheckedItem(R.id.nav_alerts);
                setTitle(getResources().getString(R.string.nav_alerts));
                break;
            case FRAGMENT_HISTORY:
                navigationView.setCheckedItem(R.id.nav_history);
                setTitle(getResources().getString(R.string.nav_history));
                break;
        }
    }

    /**
     * Sets FAB icon according to given int resource ID
     *
     * @param iconID    int ID of drawable resource to set as FAB icon
     */
    private void setFabIcon(final int iconID){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setImageDrawable(getResources().getDrawable(iconID, getTheme()));
        } else {
            //noinspection deprecation
            fab.setImageDrawable(getResources().getDrawable(iconID));
        }
    }

    /**
     * Shows a SnackBar with given text
     *
     * @param view  Parent view to show the SnackBar on.
     * @param message   String containing message to show
     */
    private static void showSnackBar(@NonNull final View view, @NonNull final String message){
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }
}