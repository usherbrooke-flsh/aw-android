package ca.uqam.espaceunaw

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import ca.uqam.espaceunaw.fragments.TestFragment
import ca.uqam.espaceunaw.fragments.WebUIFragment
import ca.uqam.espaceunaw.watcher.UsageStatsWatcher
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

private const val TAG = "MainActivity"

const val baseURL = "http://127.0.0.1:5600"


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, WebUIFragment.OnFragmentInteractionListener {

    val version: String
        get() {
            return packageManager.getPackageInfo(packageName, 0).versionName
        }

    override fun onFragmentInteraction(item: Uri) {
        Log.w(TAG, "URI onInteraction listener not implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Hide the top menu/title bar
        supportActionBar?.hide()

        nav_view.setNavigationItemSelectedListener(this)

        val ri = RustInterface(this)
        ri.startServerTask(this)

        val usw = UsageStatsWatcher(this)
        usw.setupAlarm()

        if (savedInstanceState != null) {
            return
        }
        val firstFragment = WebUIFragment.newInstance(baseURL)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, firstFragment).commit()
    }

    override fun onResume() {
        super.onResume()

        val sharedPrefsKey = getString(R.string.shared_preferences_key)
        val collectEnabledKey = getString(R.string.collect_enabled_key)
        val sharedPref = getSharedPreferences(sharedPrefsKey, MODE_PRIVATE)
        val collectEnabled = sharedPref.getBoolean(collectEnabledKey, true)

        if (collectEnabled) {
            // Ensures data is always fresh when app is opened,
            // even if it was up to an hour since the last logging-alarm was triggered.
            val usw = UsageStatsWatcher(this)
            usw.sendHeartbeats()
        }
    }

    override fun onBackPressed() {
        val webview: WebView = findViewById<WebView>(R.id.webview)
        if(webview!= null && webview.canGoBack()) {
            webview.goBack();// if there is previous page open it
        }
        else{
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setMessage("Do you want to Exit?")
            builder.setPositiveButton(
                "Yes"
            ) { dialog, which -> //if user pressed "yes", then he is allowed to exit from application
                finish()
            }
            builder.setNegativeButton(
                "No"
            ) { dialog, which -> //if user select "No", just cancel this dialog and continue with app
                dialog.cancel()
            }
            val alert = builder.create()
            alert.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        var collectEnabledKey = getString(R.string.collect_enabled_key)
        val sharedPref = getPreferences(MODE_PRIVATE)
        val collectEnabled = sharedPref.getBoolean(collectEnabledKey, true)

        var navCollectSwitch = findViewById<SwitchCompat>(R.id.nav_collect)
        navCollectSwitch.isChecked = collectEnabled
        navCollectSwitch?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> with (sharedPref.edit()) {
            putBoolean(collectEnabledKey, isChecked)
            apply()
        }}
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                Snackbar.make(coordinator_layout, "The settings button was clicked, but it's not yet implemented!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragmentClass: Class<out Fragment>? = null
        var url: String? = null

        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_dashboard -> {
                fragmentClass = TestFragment::class.java
            }
            R.id.nav_activity -> {
                fragmentClass = WebUIFragment::class.java
                url = "$baseURL/#/activity/unknown/"
            }
            R.id.nav_buckets -> {
                fragmentClass = WebUIFragment::class.java
                url = "$baseURL/#/buckets/"
            }
            R.id.nav_settings -> {
                fragmentClass = WebUIFragment::class.java
                url = "$baseURL/#/settings/"
            }
            R.id.nav_share -> {
                Snackbar.make(coordinator_layout, "The share button was clicked, but it's not yet implemented!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            R.id.nav_send -> {
                Snackbar.make(coordinator_layout, "The send button was clicked, but it's not yet implemented!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }

        val fragment: Fragment? = try {
            if (fragmentClass === WebUIFragment::class.java && url != null) {
                WebUIFragment.newInstance(url)
            } else {
                fragmentClass?.newInstance()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if(fragment != null) {
            // Insert the fragment by replacing any existing fragment
            val fragmentManager = supportFragmentManager
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
