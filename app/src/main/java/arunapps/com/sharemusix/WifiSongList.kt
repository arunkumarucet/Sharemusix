package arunapps.com.sharemusix

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import kotlinx.android.synthetic.main.activity_wifi_song_list.*

class WifiSongList : AppCompatActivity() {
    var adapter: WifiSongsAdapter? =null
    var datalist: ArrayList<String> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_song_list)
        setSupportActionBar(toolbar_list_wifi)
        progressBar.visibility= View.INVISIBLE
        val data=intent.extras.getString("songlist")
        val lines = data.split("\\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (line in lines) {
            datalist.add(line)
        }
        datalist.removeAt(0)
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditr = mypref.edit()
        prefsEditr.putBoolean("isListView",true)
        prefsEditr.apply()
        loadlistview(datalist)
    }
    fun loadlistview(data: ArrayList<String>) {
        adapter = WifiSongsAdapter(this@WifiSongList, data,progressBar)
        list_wifi_songs.adapter = adapter
    }
    override fun onBackPressed() {
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditr = mypref.edit()
        prefsEditr.putBoolean("isListView",false)
        prefsEditr.apply()
        super.onBackPressed()
    }

    override fun onPause() {
        progressBar.visibility=View.INVISIBLE
        super.onPause()
    }
    override fun onDestroy() {
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val prefsEditr = mypref.edit()
        prefsEditr.putBoolean("isListView",false)
        prefsEditr.apply()
        super.onDestroy()
    }
}
