package com.example.tieuluanandroids.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tieuluanandroids.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var main: View
    private lateinit var appBar: AppBarLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fab: FloatingActionButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        main = findViewById(R.id.main)
        appBar = findViewById(R.id.app_bar)
        toolbar = findViewById(R.id.toolbar)
        fab = findViewById(R.id.fab)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        ViewCompat.setOnApplyWindowInsetsListener(main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment_content_main
        ) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.MenuFragment,
                R.id.CalendarFragment,
                R.id.EventsFragment,
                R.id.TagManagerFragment,
                R.id.LoginFragment,
                R.id.AddHarFileFragment,
                R.id.SessionFragment,
                R.id.ApiWebViewFragment,
                R.id.AgentChatV2Fragment,
                R.id.SettingsFragment
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.MenuFragment -> {
                    if (!navController.popBackStack(R.id.MenuFragment, false)) {
                        navController.navigate(R.id.MenuFragment)
                    }
                    true
                }

                else -> {
                    if (navController.currentDestination?.id == item.itemId) {
                        true
                    } else {
                        runCatching { navController.navigate(item.itemId) }.isSuccess
                    }
                }
            }
            if (handled) {
                item.isChecked = true
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            handled
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            appBar.isVisible = true
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> {
            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.SettingsFragment)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun openNavigationDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }
}
