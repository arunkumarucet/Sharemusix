package arunapps.com.sharemusix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SeekBar
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.activity_playlist_list.*
import java.util.ArrayList

class PlaylistListActivity : AppCompatActivity(),SeekBar.OnSeekBarChangeListener {
    var adapter: PlaylistListAdapter? = null
    var playlist_name: String? = null
    private var slidingUpPanelLayout: SlidingUpPanelLayout? = null
    var receiverElapsedTime: BroadcastReceiver? = null
    var receiverCompleted: BroadcastReceiver? = null
    var elapsedTime: Int=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_list)
        setSupportActionBar(toolbar_playlist_list)
        playlist_name=intent.extras.getString("playlist_name")
        title=playlist_name
        playlistname.text=playlist_name
        loadlistview(playlist_name!!)
        justifyListViewHeightBasedOnChildren()
        playlistname.isSelected=true
        slidingUpPanelLayout= sliding_layout as SlidingUpPanelLayout
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        scrollview_playlist.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = scrollview_playlist.scrollY
            if (scrollY == 0) toolbar_playlist_list.background=resources.getDrawable(R.drawable.toolbar_gradient)
            else toolbar_playlist_list.background=resources.getDrawable(R.drawable.toolbar_gradient_onup)
        }
        floatingActionButton.setOnClickListener{
            try {
                DatabaseHandler(this@PlaylistListActivity).deletecurrent()
                val db = DatabaseHandler(applicationContext)
                val fname: ArrayList<String> = db.getPlayList(1, playlist_name!!)
                val artist: ArrayList<String> = db.getPlayList(2, playlist_name!!)
                val album: ArrayList<String> = db.getPlayList(3, playlist_name!!)
                val path: ArrayList<String> = db.getPlayList(4, playlist_name!!)
                var i1 = 0
                for (i in 0 until fname.size) {
                    DatabaseHandler(this@PlaylistListActivity).insertcurrent_normal(i1, fname[i], artist[i], album[i], path[i])
                    i1++
                }
                val mypref = PreferenceManager.getDefaultSharedPreferences(this@PlaylistListActivity)
                val prefsEditr = mypref.edit()
                prefsEditr.putInt("position", 0)
                prefsEditr.apply()
                val serviceIntent = Intent(this@PlaylistListActivity, PlayMusic::class.java)
                serviceIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startService(serviceIntent)
                stopService(serviceIntent)
                serviceIntent.action = Constants.ACTION.STARTFOREGROUND_ACTION
                startService(serviceIntent)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
        seekBar.progress=0
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
                    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    buttonbottom.setImageResource(R.drawable.ic_playlist_dark)
                }
                if (newState.toString() === "COLLAPSED") {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    buttonbottom.setImageResource(R.drawable.ic_play_dark_1)
                }
            }
        })
        receiverElapsedTime = object:BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                elapsedTime = intent.getIntExtra(PlayMusic.MPS_MESSAGE, 0)
                updateElapsedTime(elapsedTime)
                present_time.text=secondsToString(elapsedTime)
                val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val position = mypref.getInt("position", 0)
                initInfos(DatabaseHandler(applicationContext).getcurrentlist(4)[position])
                name.text=DatabaseHandler(applicationContext).getcurrentlist(1)[position]
                artist.text=DatabaseHandler(applicationContext).getcurrentlist(2)[position]
            }
        }
        receiverCompleted = object:BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
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
                serviceIntent.action = Constants.ACTION.PLAY_WITH_SEEK
                serviceIntent.putExtra("seekpos",0)
                startService(serviceIntent)
            }
        }
        buttonbottom.setOnClickListener {
            if(slidingUpPanelLayout!!.panelState.toString()=="COLLAPSED"){
                val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
                try{
                    serviceIntent.action = Constants.ACTION.PLAY_ACTION
                    startService(serviceIntent)
                }catch (e: Exception){
                    serviceIntent.action = Constants.ACTION.PLAY_WITH_SEEK
                    serviceIntent.putExtra("seekpos",0)
                    startService(serviceIntent)
                }
            }else{
                startActivity(Intent(applicationContext,SlideAndDragListViewActivity1::class.java))
            }
        }
        play_pause.isEnabled=true
        seekBar.setOnSeekBarChangeListener(this)

    }
    fun loadlistview(playlistname: String) {
        val db = DatabaseHandler(this@PlaylistListActivity)
        val path = db.getPlayList(4,playlistname)
        adapter = PlaylistListAdapter(this@PlaylistListActivity, db.getPlayList(1,playlistname), db.getPlayList(2,playlistname), db.getPlayList(3,playlistname), path,playlistname)
        playlist_listview.adapter = adapter
        val mData = MediaMetadataRetriever()
        var songImage: Bitmap? = null
        try {
            mData.setDataSource(path[0])
            val art = mData.embeddedPicture
            songImage = BitmapFactory.decodeByteArray(art, 0, art.size)
            songimage_playlist.setImageBitmap(songImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    fun justifyListViewHeightBasedOnChildren() {
        val adapter: ListAdapter = playlist_listview.adapter
        var totalHeight = 0
        for (i in 0 until adapter.count) {
            val listItem = adapter.getView(i, null, playlist_listview)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }
        val par = playlist_listview.layoutParams
        par.height = totalHeight + playlist_listview.dividerHeight * (adapter.count - 1)
        playlist_listview.layoutParams=par
        playlist_listview.requestLayout()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
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
    private fun secondsToString(time: Int): String {
        var time = time
        time /= 1000
        return String.format("%2d:%02d", time / 60, time % 60)
    }
    private fun initInfos(path: String) {
        if (path != null) {
            val mData = MediaMetadataRetriever()
            mData.setDataSource(path)
            val duration: Int = Integer.parseInt(mData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
            seekBar.max=duration
            total_time.text=secondsToString(duration)
            try {
                mData.setDataSource(path)
                val art= mData.embeddedPicture
                val songImage = BitmapFactory.decodeByteArray(art, 0, art.size)
                imageView.setImageBitmap(songImage)
                songimage.setImageBitmap(songImage)
            } catch (e:Exception) {
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
    }
}
