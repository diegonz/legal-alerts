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

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.smartidea.android.legalalerts.broadcastReceivers.AlertsAlarmReceiver;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // Integer Fragment identifiers
    public static final int FRAGMENT_ALERTS = 0;
    public static final int FRAGMENT_HISTORY = 1;
    // runningFragment initialized to -1, forcing first replacement and runningFragment update.
    private int runningFragment = -1;
    // Running fragment string
    private static final String DIALOG_TAG = "dialog_legal_alerts";
    private static final String RUNNING_FRAGMENT_STRING = "running_fragment";
    private static final String SET_ALARM_FROM_ACTIVITY = AlertsAlarmReceiver.SET_ALARM_FROM_ACTIVITY;
    // ButterKnife bindings
    @Bind(R.id.nav_view) NavigationView navigationView;
    @Bind(R.id.drawer_layout) DrawerLayout drawer;
    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.fab) FloatingActionButton fab;

    @OnClick(R.id.fab)
    void fabClickListener() {
        switch (runningFragment) {
            case FRAGMENT_ALERTS:
                new LegalAlertsDialog().show(getSupportFragmentManager(), DIALOG_TAG);
                break;
            case FRAGMENT_HISTORY:
                break;
        }
    }

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
        // Hide fab button
        fab.hide();

        if (savedInstanceState == null) {
            // Check/start new alarm
            sendBroadcast(new Intent(this, AlertsAlarmReceiver.class)
                    .setAction(SET_ALARM_FROM_ACTIVITY)
            );
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
        setDrawerCheckedItemAndTitle();
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
                new LegalAlertsDialog().show(getSupportFragmentManager(), DIALOG_TAG);
                break;
            case START_SETTINGS_ACTIVITY:
                // Launch Settings Activity
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

        // Check if runningFragment is the same received
        if (runningFragment != fragmentID){
            Fragment fragment = null;
            Class fragmentClass;

            switch (fragmentID) {
                case FRAGMENT_ALERTS:
                    // Show fab button
                    fab.show();
                    fragmentClass = AlertsFragment.class;
                    break;
                case FRAGMENT_HISTORY:
                    // Hide fab button
                    fab.hide();
                    fragmentClass = HistoryFragment.class;
                    break;
                default:
                    // Show fab button
                    fab.show();
                    fragmentClass = AlertsFragment.class;
                    break;
            }
            try {
                fragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Insert the fragment by replacing any existing fragment
            FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
            beginTransaction.setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
            );
            beginTransaction.replace(R.id.fragmentMainPlaceholder, fragment);
            beginTransaction.commit();
            // Change running fragment id
            runningFragment = fragmentID;
        }
        setDrawerCheckedItemAndTitle();
    }

    private void setDrawerCheckedItemAndTitle(){
        // Select item on drawer
        switch (runningFragment) {
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
}