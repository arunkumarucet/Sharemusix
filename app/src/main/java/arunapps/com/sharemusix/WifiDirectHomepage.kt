package arunapps.com.sharemusix

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_wifi_direct_homepage.*

class WifiDirectHomepage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_direct_homepage)
        setSupportActionBar(toolbar2)
        val wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager != null)
            wifiManager.isWifiEnabled = true
        val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefsEditr = mypref.edit()
        stream.setOnClickListener {
            prefsEditr.putBoolean("isStream",true)
            prefsEditr.apply()
            startActivity(Intent(applicationContext,WiFiDirectActivity::class.java))
            finish()
        }
        listen.setOnClickListener {
            prefsEditr.putBoolean("isStream",false)
            prefsEditr.apply()
            startActivity(Intent(applicationContext,WiFiDirectActivity::class.java))
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.wifihomepage, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.profile) {
            val inflater = layoutInflater
            val alertLayout = inflater.inflate(R.layout.layout_custom_dialog_wifidirect, null)
            val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val text = alertLayout.findViewById<EditText>(R.id.device_name) as EditText
            text.setText(mypref.getString("devicename",Build.MANUFACTURER+" "+Build.MODEL))
            val alert = AlertDialog.Builder(ContextThemeWrapper(this, R.style.myDialog))
            alert.setTitle("Profile Name")
            // this is set the view from XML inside AlertDialog
            alert.setView(alertLayout)
            // disallow cancel of AlertDialog on click of back button and outside touch
            alert.setCancelable(false)
            alert.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> })

            alert.setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->
                val prefsEditr = mypref.edit()
                prefsEditr.putString("devicename",text.text.toString())
                prefsEditr.apply()
            })
            val dialog = alert.create()
            dialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
