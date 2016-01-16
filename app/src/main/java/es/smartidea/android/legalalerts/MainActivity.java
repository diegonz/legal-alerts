package es.smartidea.android.legalalerts;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import es.smartidea.android.legalalerts.broadcastReceivers.AlertsAlarmReceiver;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Integer Fragment identifiers
    public static final int FRAGMENT_ALERTS = 0;
    public static final int FRAGMENT_HISTORY = 1;
    // RUNNING_FRAGMENT initialized to -1, forcing first replacement and RUNNING_FRAGMENT update.
    private int RUNNING_FRAGMENT = -1;
    // Running fragment string
    static final String RUNNING_FRAGMENT_STRING = "running_fragment";
    private FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int startOnFragment;

        // Check activity recreation
        if (savedInstanceState != null) {
            startOnFragment = savedInstanceState.getInt(RUNNING_FRAGMENT_STRING);
        } else {
            // Get Intent extras from the intent which started activity
            startOnFragment = getIntent().getIntExtra("start_on_fragment", FRAGMENT_ALERTS);
            // Check/set the Alerts alarm if cold start
            setAlertsAlarmFromActivity();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Set Drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get fab reference for showing/hiding purposes
        fab = (FloatingActionButton) findViewById(R.id.fab);
        // Hide fab button
        fab.hide();

        // Replace fragment according to intent extras or savedInstanceState
        // If starting from scratch it defaults to FRAGMENT_ALERTS
        replaceFragment(startOnFragment);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the user's current running fragment
        outState.putInt(RUNNING_FRAGMENT_STRING, RUNNING_FRAGMENT);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Start Settings activity
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        final int START_DIALOG_ALERT = 1;
        final int START_SETTINGS_ACTIVITY = 2;
        int afterSelectionTask = 0;

        if (id == R.id.nav_alerts) {
            replaceFragment(FRAGMENT_ALERTS);
            RUNNING_FRAGMENT = FRAGMENT_ALERTS;
        } else if (id == R.id.nav_add_alert) {
            replaceFragment(FRAGMENT_ALERTS);
            // Set start dialog flag after replacing
            afterSelectionTask = START_DIALOG_ALERT;
        } else if (id == R.id.nav_history) {
            replaceFragment(FRAGMENT_HISTORY);
            RUNNING_FRAGMENT = FRAGMENT_HISTORY;
        } else if (id == R.id.nav_settings) {
            afterSelectionTask = START_SETTINGS_ACTIVITY;
        } else if (id == R.id.nav_share) {
            replaceFragment(FRAGMENT_ALERTS);
            RUNNING_FRAGMENT = FRAGMENT_ALERTS;
        } else if (id == R.id.nav_info) {
            replaceFragment(FRAGMENT_ALERTS);
            RUNNING_FRAGMENT = FRAGMENT_ALERTS;
        }

        // Highlight the selected item and update the title if no afterSelectionTask.
        // Then close the drawer
        if (afterSelectionTask == 0){
            item.setChecked(true);
            setTitle(item.getTitle());
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        // Launch afterSelectionTask if there is any
        switch (afterSelectionTask){
            case START_DIALOG_ALERT:
                // Launch Alerts dialog
                AlertDialog alertDialog = new AlertDialog();
                alertDialog.show(getSupportFragmentManager(), "dialog_alert");
                break;
            case START_SETTINGS_ACTIVITY:
                // Launch Settings Activity
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // TODO: Make a selection on drawer
        // Get Intent extras and start Fragment replacing
        replaceFragment(intent.getIntExtra("start_on_fragment", FRAGMENT_ALERTS));
    }

    /**
     * replaceFragment (final int fragmentID)
     *
     * Replaces Fragment given by fragmentID on main fragment placeholder
     *
     * @param fragmentID : Numeric ID of objective fragment
     *
     **/
    public void replaceFragment(final int fragmentID){

        String VIEW_TITLE;
        // Check if RUNNING_FRAGMENT is the same received
        if (RUNNING_FRAGMENT != fragmentID){
            Fragment fragment = null;
            Class fragmentClass;

            switch (fragmentID) {
                case FRAGMENT_ALERTS:
                    // Show fab button
                    fab.show();
                    fragmentClass = AlertsFragment.class;
                    VIEW_TITLE = getResources().getString(R.string.nav_alerts);
                    break;
                case FRAGMENT_HISTORY:
                    // Hide fab button
                    fab.hide();
                    fragmentClass = HistoryFragment.class;
                    VIEW_TITLE = getResources().getString(R.string.nav_history);
                    break;
                default:
                    // Show fab button
                    fab.show();
                    fragmentClass = AlertsFragment.class;
                    VIEW_TITLE = getResources().getString(R.string.nav_alerts);
                    break;
            }
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Insert the fragment by replacing any existing fragment
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragmentMainPlaceholder, fragment);
            fragmentTransaction.commit();
            setTitle(VIEW_TITLE);
            RUNNING_FRAGMENT = fragmentID;
        }
    }

    // setAlertsAlarmFromActivity void method, sends broadcast to setup alarm
    private void setAlertsAlarmFromActivity(){
        Intent setAlarmIntent = new Intent(this, AlertsAlarmReceiver.class).setAction(
                AlertsAlarmReceiver.SET_ALARM_FROM_ACTIVITY);
        sendBroadcast(setAlarmIntent);
    }
}