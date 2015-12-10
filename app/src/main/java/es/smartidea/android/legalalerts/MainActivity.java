package es.smartidea.android.legalalerts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.NotificationCompat;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AlertsFragment.OnFragmentInteractionListener {

    // Simple running fragment identifiers
    private static final int FRAGMENT_ALERTS = 0;
    private static final int FRAGMENT_HISTORY = 1;
    private int RUNNING_FRAGMENT = 0;
    private String VIEW_TITLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Intent extras
        int initOnFragment = getIntent().getIntExtra("initOnFragment", FRAGMENT_ALERTS);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // TODO: Setup FAB button on each fragment programmatically

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set Fragment depending on Intent extras
        Class fragmentInitClass;

        switch (initOnFragment){
            case FRAGMENT_ALERTS:
                fragmentInitClass = AlertsFragment.class;
                VIEW_TITLE = getResources().getString(R.string.app_name);
                break;
            case FRAGMENT_HISTORY:
                fragmentInitClass = HistoryFragment.class;
                VIEW_TITLE = getResources().getString(R.string.nav_history);
                break;
            default:
                fragmentInitClass = AlertsFragment.class;
                VIEW_TITLE = getResources().getString(R.string.app_name);
                break;
        }

        Fragment fragment = null;
        try {
            fragment = (Fragment) fragmentInitClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentTransaction initFragment = getSupportFragmentManager().beginTransaction();
        initFragment.replace(R.id.fragmentMainPlaceholder, fragment, VIEW_TITLE);
        initFragment.commit();
        // TODO: Check title
        setTitle(VIEW_TITLE);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment = null;
        Class fragmentClass = AlertsFragment.class;
        Boolean startDialogAlert = false;

        if (id == R.id.nav_alerts) {
            fragmentClass = AlertsFragment.class;
            RUNNING_FRAGMENT = FRAGMENT_ALERTS;
        } else if (id == R.id.nav_add_alert) {
            fragmentClass = AlertsFragment.class;
            // Set start dialog flag TODO: Check alternatives (frag listener?)
            startDialogAlert = true;
        } else if (id == R.id.nav_history) {
            fragmentClass = HistoryFragment.class;
            RUNNING_FRAGMENT = FRAGMENT_HISTORY;
        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragmentMainPlaceholder, fragment).commit();

        // Highlight the selected item, update the title, and close the drawer
        item.setChecked(true);
        setTitle(item.getTitle());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        // Start dialog Alert TODO: Check alternatives (frag listener?)
        if (startDialogAlert){
            DialogAlert dialogAlert = new DialogAlert();
            dialogAlert.show(fragmentManager, "dialog_alert");
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("Intent", "Intent received!");
        super.onNewIntent(intent);
        // Get Intent extras
        int initOnFragment = getIntent().getIntExtra("initOnFragment", FRAGMENT_ALERTS);
        replaceFragment(initOnFragment);
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

        // Check if RUNNING_FRAGMENT is the same received
        if (RUNNING_FRAGMENT != fragmentID){
            Fragment fragment = null;
            Class fragmentClass;

            switch (fragmentID) {
                case FRAGMENT_ALERTS:
                    fragmentClass = AlertsFragment.class;
                    VIEW_TITLE = getResources().getString(R.string.nav_alerts);
                    break;
                case FRAGMENT_HISTORY:
                    fragmentClass = HistoryFragment.class;
                    VIEW_TITLE = getResources().getString(R.string.nav_history);
                    break;
                default:
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
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentMainPlaceholder, fragment)
                    .commit();
            RUNNING_FRAGMENT = fragmentID;
        }
    }

    @Override
    public void onClickedAddButton(String title, String message) {
        showAlertNotification(title, message);
    }

    /**
     * Void method showAlertNotification(String title, String message)
     * Shows notification according to given parameters
     * String title
     * String message
    * */
    public void showAlertNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("initOnFragment", FRAGMENT_HISTORY);
        /**
         * Reuse MainActivity if possible (ex: is running, paused...)
         * also can use "Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED"
         * "FLAG_ACTIVITY_CLEAR_TOP" "FLAG_ACTIVITY_SINGLE_TOP" and others.
         * check: http://developer.android.com/intl/es/reference/android/content/Intent.html#constants
         */
//        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Resources resources = getResources();
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker(resources.getString(R.string.app_name))
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }
}
