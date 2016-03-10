package es.smartidea.android.legalalerts;

import android.content.ContentValues;
import android.content.Context;
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
import es.smartidea.android.legalalerts.database.DBHelper;
import es.smartidea.android.legalalerts.receivers.AlarmReceiver;
import es.smartidea.android.legalalerts.database.DBContentProvider;
import es.smartidea.android.legalalerts.database.DBContract;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static Uri ALERTS_URI = DBContentProvider.ALERTS_URI;
    private final static Uri HISTORY_URI = DBContentProvider.HISTORY_URI;
    private final static String WHERE_NAME_EQUALS = DBContract.Alerts.COL_ALERT_NAME + '=';
    private final static String DIALOG_TAG = "dialog_legal_alerts";
    private final static String RUNNING_FRAGMENT_STRING = "running_fragment";
    public final static int FRAGMENT_ALERTS_ID = 0;
    public final static int FRAGMENT_HISTORY_ID = 1;
    private int runningFragment = -1;
    @Bind(R.id.nav_view) NavigationView navigationView;
    @Bind(R.id.drawer_layout) DrawerLayout drawer;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fab) FloatingActionButton fab;
    @OnClick(R.id.fab)
    void fabClickListener(View view) {
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

    /*
    * LIFECYCLE START
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
            sendBroadcast(new Intent(this, AlarmReceiver.class));
            // Starting from scratch, getIntExtra from intent to replace
            // corresponding fragment, defaulting to FRAGMENT_ALERTS_ID
            replaceFragment(getIntent().getIntExtra("start_on_fragment", FRAGMENT_ALERTS_ID));
        } else {
            // Replace fragment according to intent extras or savedInstanceState
            // If starting from scratch it defaults to FRAGMENT_ALERTS_ID
            replaceFragment(savedInstanceState.getInt(RUNNING_FRAGMENT_STRING, FRAGMENT_ALERTS_ID));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Check intent extras and start fragment replacing
        if (intent.hasExtra("start_on_fragment")) {
            replaceFragment(intent.getIntExtra("start_on_fragment", FRAGMENT_ALERTS_ID));
        }
        setDrawerCheckedItemAndTitle(runningFragment);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the user's current running fragment
        outState.putInt(RUNNING_FRAGMENT_STRING, runningFragment);
    }

    /*
    * LIFECYCLE END
    * */

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
                replaceFragment(FRAGMENT_ALERTS_ID);
                break;
            case R.id.nav_add_alert:
                afterSelectionTask = START_DIALOG_ALERT;
                break;
            case R.id.nav_history:
                replaceFragment(FRAGMENT_HISTORY_ID);
                break;
            case R.id.nav_settings:
                afterSelectionTask = START_SETTINGS_ACTIVITY;
                break;
            case R.id.nav_share:
//            replaceFragment(FRAGMENT_ALERTS_ID);
                break;
            case R.id.nav_info:
//            replaceFragment(FRAGMENT_ALERTS_ID);
                break;
        }

        // Launch afterSelectionTask if there is any
        switch (afterSelectionTask){
            case START_DIALOG_ALERT:
                // Launch Alerts dialog
                new LegalAlertDialog().show(getSupportFragmentManager(), DIALOG_TAG);
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
    public static int insertNewAlert(final Context context, final String alertName,
                                     final boolean isLiteralSearch){
        ContentValues values = new ContentValues();
        values.put(DBContract.Alerts.COL_ALERT_NAME, alertName);
        // Get REVERSE toggle button state by casting a boolean with ternary operator expression.
        // If checked returns 0, if not returns 1
        int literalSearchIntValue = (isLiteralSearch) ? 0 : 1;
        values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
        Uri resultID = context.getContentResolver().insert(ALERTS_URI, values);
        if (resultID != null) {
            // Return last item of Uri (ID)
            return Integer.parseInt(resultID.getLastPathSegment());
        }
        // If not inserted return -1
        return -1;
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
    public static int updateAlert(final Context context, final String oldName, final String newName,
                                  final boolean isLiteralSearch){
        ContentValues values = new ContentValues();
        values.put(DBContract.Alerts.COL_ALERT_NAME, newName);
        // Get REVERSE toggle button state by casting a boolean with ternary operator expression.
        // If checked returns 0, if not returns 1
        int literalSearchIntValue = (isLiteralSearch) ? 0 : 1;
        values.put(DBContract.Alerts.COL_ALERT_SEARCH_NOT_LITERAL, literalSearchIntValue);
        // Set WHERE clause between single quotes to avoid being
        // identified by SQLite as a table +info: http://stackoverflow.com/a/13173792/3799840
        return context.getContentResolver()
                .update(ALERTS_URI, values, WHERE_NAME_EQUALS + '\'' + oldName + '\'', null) < 1
                ? -1
                : 1;
    }

    /**
     * Activity method to delete Alerts into Database according to given alertName
     *
     * @param context   Context to get ContentResolver
     * @param alertName String representing alertName to be deleted
     * @return int representing number of deleted items
     */
    public static int deleteAlert(final Context context, final String alertName){
        return context.getContentResolver().delete(
                ALERTS_URI,
                DBContract.Alerts.COL_ALERT_NAME + "='" + alertName + '\'',
                null
        );
    }

    /**
     * Delete items from history table according to given parameters
     *
     * @param context       Context to get ContentResolver
     * @param documentName  @Nullable String representing history item to delete
     *                      if NULL received deleted all entries in the table
     * @param alertName     @Nullable String representing history item to delete
     *                      if NULL received deleted all entries in the table
     * @return  int indicating number of items deleted. -1 If no action taken.
     */
    public static int deleteHistory(final Context context, @Nullable final String documentName,
                                    @Nullable final String alertName){
        if (!(documentName == null) && !(alertName == null)){
            // Construct WHERE statement
            //noinspection StringConcatenationMissingWhitespace
            final String WHERE = DBHelper.SPACE_OPEN_BRACKET +
                    DBContract.History.COL_HISTORY_DOCUMENT_NAME + "='" + documentName + '\'' +
                    DBHelper.CLOSE_BRACKET_SPACE + " AND " + DBHelper.SPACE_OPEN_BRACKET +
                    DBContract.History.COL_HISTORY_RELATED_ALERT_NAME + "='" + alertName + '\'' +
                    DBHelper.CLOSE_BRACKET_SPACE;
            // Delete concrete alert
            return context.getContentResolver().delete(HISTORY_URI, WHERE, null);

        } else if (documentName == null && alertName == null) {
            // Delete all history records
            return context.getContentResolver().delete(HISTORY_URI, null, null);
        } else return -1; // Return -1 as error flag indicating no order was processed
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
                    case FRAGMENT_ALERTS_ID:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(
                                        R.id.fragmentMainPlaceholder,
                                        AlertsFragment.class.newInstance())
                                .commit();
                        // Setup FAB button after replacing
                        setupFabButton(android.R.drawable.ic_input_add, R.string.fab_description_alerts);
                        break;
                    case FRAGMENT_HISTORY_ID:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                .replace(R.id.fragmentMainPlaceholder,
                                        HistoryFragment.class.newInstance())
                                .commit();
                        // Set FAB icon after replacing
                        setupFabButton(android.R.drawable.ic_menu_delete, R.string.fab_description_history);
                        break;
                    default:
                        getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                                .replace(R.id.fragmentMainPlaceholder,
                                        AlertsFragment.class.newInstance())
                                .commit();
                        // Setup FAB button after replacing
                        setupFabButton(android.R.drawable.ic_input_add, R.string.fab_description_alerts);
                        break;
                }
                // Change running fragment id
                runningFragment = fragmentID;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Set drawer and title after replacing
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
     * Sets FAB icon according to given int resource ID
     *
     * @param iconID    int ID of drawable resource to set as FAB icon
     * @param contentDescription    int ID of string resource to set
     *                              as FAB´s content description
     */
    private void setupFabButton(final int iconID, final int contentDescription){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setImageDrawable(getResources().getDrawable(iconID, getTheme()));
        } else {
            //noinspection deprecation
            fab.setImageDrawable(getResources().getDrawable(iconID));
        }
        // Set content description
        fab.setContentDescription(getString(contentDescription));
    }

    /**
     * Shows a SnackBar with given text
     *
     * @param view  Parent view to show the SnackBar on.
     * @param message   String containing message to show
     */
    public static void showSnackBar(@NonNull final View view, @NonNull final String message){
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }
}