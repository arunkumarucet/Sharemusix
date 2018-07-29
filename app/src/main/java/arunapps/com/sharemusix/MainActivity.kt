package arunapps.com.sharemusix

import android.Manifest
import android.content.*
import android.net.Uri
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle

import kotlinx.android.synthetic.main.activity_main.*
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import android.support.v7.app.AlertDialog
import android.view.*
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.widget.*
import android.media.AudioManager.STREAM_MUSIC
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager


class MainActivity : AppCompatActivity(),Playlist.OnFragmentInteractionListener,Songs.OnFragmentInteractionListener,Album.OnFragmentInteractionListener,Artist.OnFragmentInteractionListener,SeekBar.OnSeekBarChangeListener{
    private var slidingUpPanelLayout: SlidingUpPanelLayout? = null
    var receiverElapsedTime:BroadcastReceiver? = null
    var receiverCompleted:BroadcastReceiver? = null
    var elapsedTime: Int=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val intent = intent
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data
            val db = DatabaseHandler(applicationContext)
            val metaRetriver = MediaMetadataRetriever()
            try {
                metaRetriver.setDataSource(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                db.deletecurrent()
                db.insertcurrent_normal(0, metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST), metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), uri.toString())
            } catch (e: Exception) {
                db.deletecurrent()
                db.insertcurrent_normal(0, "Unknown Song", "Unknown Artist", "Unknown Album", uri.toString())
            }
            val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val prefsEditr = mypref.edit()
            prefsEditr.putInt("position", 0)
            prefsEditr.apply()
            val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
            serviceIntent.action = "arunapps.com.sharemusix.action.stopforeground"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startService(serviceIntent)
            stopService(serviceIntent)
            serviceIntent.action = "arunapps.com.sharemusix.action.startforeground"
            startService(serviceIntent)
        }
        slidingUpPanelLayout= sliding_layout as SlidingUpPanelLayout
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        loadview()
        seekBar.progress=0
        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        try {
            val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val position = mypref.getInt("position", 0)
            initInfos(DatabaseHandler(applicationContext).getcurrentlist(4)[position])
            name.text = DatabaseHandler(applicationContext).getcurrentlist(1)[position]
            artist.text = DatabaseHandler(applicationContext).getcurrentlist(2)[position]
        }catch(e:Exception){
            e.printStackTrace()
        }
        slidingUpPanelLayout!!.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {

            }
            override fun onPanelStateChanged(panel: View, previousState: SlidingUpPanelLayout.PanelState, newState: SlidingUpPanelLayout.PanelState) {
                if (newState.toString() === "EXPANDED") {
                    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    buttonbottom.setImageResource(R.drawable.ic_playlist_dark)
                }
                if (newState.toString() === "COLLAPSED") {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    buttonbottom.setImageResource(R.drawable.ic_play_dark_1)
                }
            }
        })

        receiverElapsedTime = object:BroadcastReceiver() {
            override fun onReceive(context:Context, intent:Intent) {
                elapsedTime = intent.getIntExtra(PlayMusic.MPS_MESSAGE, 0)
                updateElapsedTime(elapsedTime)
                present_time.text=secondsToString(elapsedTime)
            }
        }
        receiverCompleted = object:BroadcastReceiver() {
            override fun onReceive(context:Context,intent:Intent) {
                val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val position = mypref.getInt("position", 0)
                initInfos(DatabaseHandler(applicationContext).getcurrentlist(4)[position])
                name.text=DatabaseHandler(applicationContext).getcurrentlist(1)[position]
                artist.text=DatabaseHandler(applicationContext).getcurrentlist(2)[position]
                play_pause.setImageResource(R.drawable.ic_play_icon)
            }
        }
        next.setOnClickListener {
            val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
            serviceIntent.action = Constants.ACTION.NEXT_ACTION
            startService(serviceIntent)
        }
        previous.setOnClickListener {
            val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
            serviceIntent.action = Constants.ACTION.PREV_ACTION
            startService(serviceIntent)
        }
        play_pause.setOnClickListener {
            val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
            try{
            serviceIntent.action = Constants.ACTION.PLAY_ACTION
            startService(serviceIntent)
            }catch (e: Exception){
                try {
                    serviceIntent.action = Constants.ACTION.PLAY_WITH_SEEK
                    serviceIntent.putExtra("seekpos", 0)
                    startService(serviceIntent)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        buttonbottom.setOnClickListener {
            if(slidingUpPanelLayout!!.panelState.toString()=="COLLAPSED"){
                val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
                try{
                    serviceIntent.action = Constants.ACTION.PLAY_ACTION
                    startService(serviceIntent)
                }catch (e: Exception){
                    try {
                        serviceIntent.action = Constants.ACTION.PLAY_WITH_SEEK
                        serviceIntent.putExtra("seekpos", 0)
                        startService(serviceIntent)
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }else{
                startActivity(Intent(applicationContext,SlideAndDragListViewActivity1::class.java))
            }
        }

        play_pause.isEnabled=true
        seekBar.setOnSeekBarChangeListener(this)
    }

    private fun loadview(){
        val mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        mSectionsPagerAdapter.addFragment(Playlist(),"Playlists")
        mSectionsPagerAdapter.addFragment(Songs(),"Songs")
        mSectionsPagerAdapter.addFragment(Artist(),"Artists")
        mSectionsPagerAdapter.addFragment(Album(),"Albums")
        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter
    }
    override fun onFragmentInteraction(uri: Uri) {
    }

    override fun onStart() {
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiverElapsedTime, IntentFilter(PlayMusic.MPS_RESULT))
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiverCompleted, IntentFilter(PlayMusic.MPS_COMPLETED))
        super.onStart()
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiverElapsedTime)
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiverCompleted)
        super.onStop()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                   fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
        serviceIntent.action = Constants.ACTION.PLAY_WITH_SEEK
        serviceIntent.putExtra("seekpos",seekBar.progress)
        startService(serviceIntent)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_wifi) {
            val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val isConnected = mypref.getBoolean("isWiFiConnected",false)
            if(isConnected) {
                startActivity(Intent(applicationContext,WiFiDirectActivity::class.java))
            }
            else{
                startActivity(Intent(applicationContext, WifiDirectHomepage::class.java))
            }
            return true
        }
        if(id == R.id.action_create_playlist){
            val inflater = layoutInflater
            val alertLayout = inflater.inflate(R.layout.layout_custom_dialog_newplaylist, null)
            val text = alertLayout.findViewById<EditText>(R.id.playname) as EditText
            val alert = AlertDialog.Builder(ContextThemeWrapper(this, R.style.myDialog))
            alert.setTitle("New Playlist")
            // this is set the view from XML inside AlertDialog
            alert.setView(alertLayout)
            // disallow cancel of AlertDialog on click of back button and outside touch
            alert.setCancelable(false)
            alert.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> })

            alert.setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->
                val db = DatabaseHandler(applicationContext)
                if(db.getPlaylistMain(0).contains(text.text.toString()))
                    Toast.makeText(applicationContext,"Playlist Already Exists!",Toast.LENGTH_LONG).show()
                else
                    db.insertplaylistmain(text.text.toString())
                loadview()
            })
            val dialog = alert.create()
            dialog.show()
            return true
        }
        if(id == R.id.action_3d){
            val mypref = PreferenceManager.getDefaultSharedPreferences(this)
            val isthreedenabled = mypref.getBoolean("isthreedenabled",false)
            val prefsEditr = mypref.edit()
            if(isthreedenabled){
                prefsEditr.putBoolean("isthreedenabled",false)
                prefsEditr.apply()
                Toast.makeText(applicationContext,"3D Effect Disabled",Toast.LENGTH_SHORT).show()
            }else{
                prefsEditr.putBoolean("isthreedenabled",true)
                prefsEditr.apply()
                Toast.makeText(applicationContext,"3D Effect Enabled",Toast.LENGTH_LONG).show()
            }
        }
        if(id == R.id.action_help){
            val alert = AlertDialog.Builder(ContextThemeWrapper(this, R.style.myDialog))
            alert.setTitle("Help")
            alert.setMessage("WiFi Streaming issues:\n \t When wifi is not streaming or having any problem related to it then Go to Setting->Apps->Sharemusix->Storage and select Clear Data. Now try to stream songs, if problem still exists Restart phone. Otherwise mail us about your issues to us at contact@arunapps.ml")
            alert.setCancelable(false)
            alert.setPositiveButton("Close", DialogInterface.OnClickListener { dialog, which ->
            })
            val dialog = alert.create()
            dialog.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun secondsToString(time: Int): String {
        var time = time
        time /= 1000
        return String.format("%2d:%02d", time / 60, time % 60)
    }
    private fun initInfos(path: String) {
        if (path != null) {
            val mData = MediaMetadataRetriever()
            try {
                mData.setDataSource(path)
                val duration: Int = Integer.parseInt(mData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
                seekBar.max = duration
                total_time.text = secondsToString(duration)
                try {
                    mData.setDataSource(path)
                    val art = mData.embeddedPicture
                    val songImage = BitmapFactory.decodeByteArray(art, 0, art.size)
                    imageView.setImageBitmap(songImage)
                    songimage.setImageBitmap(songImage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }
    private fun updateElapsedTime(elapsedTime: Int) {
        seekBar.progress=elapsedTime
        play_pause.isEnabled=true
        if(PlayMusic.PLAYING_STATE) {
            play_pause.setImageResource(R.drawable.ic_pause_icon)
            if(slidingUpPanelLayout!!.panelState.toString()=="COLLAPSED")
                buttonbottom.setImageResource(R.drawable.ic_pause_dark_1)

        }
        else {
            play_pause.setImageResource(R.drawable.ic_play_icon)
            if(slidingUpPanelLayout!!.panelState.toString()=="COLLAPSED")
                buttonbottom.setImageResource(R.drawable.ic_play_dark_1)
        }
        val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val position = mypref.getInt("position", 0)
        initInfos(DatabaseHandler(applicationContext).getcurrentlist(4)[position])
        name.text=DatabaseHandler(applicationContext).getcurrentlist(1)[position]
        artist.text=DatabaseHandler(applicationContext).getcurrentlist(2)[position]
    }
    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val mFragmentList = ArrayList<Fragment>()
        private val mFragmentTitleList = ArrayList<String>()
        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }
        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }

}
