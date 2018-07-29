package arunapps.com.sharemusix

import android.content.*
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_wifi_current_listener.*

class WifiCurrentListener : AppCompatActivity(),SeekBar.OnSeekBarChangeListener {
    var receiverElapsedTime: BroadcastReceiver? = null
    var receiverCompleted: BroadcastReceiver? = null
    var elapsedTime: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_current_listener)
        val result = intent.extras!!.getString("result")
        val db = DatabaseHandler(applicationContext)
        val metaRetriver = MediaMetadataRetriever()
        try {
            metaRetriver.setDataSource(result)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            db.deletecurrent()
            db.insertcurrent_normal(0, metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST), metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), result!!)
        } catch (e: Exception) {
            db.deletecurrent()
            db.insertcurrent_normal(0, "Unknown Song", "Unknown Artist", "Unknown Album", result!!)
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
        receiverElapsedTime = object:BroadcastReceiver() {
            override fun onReceive(context: Context, intent:Intent) {
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
            override fun onReceive(context: Context, intent:Intent) {
                val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val position = mypref.getInt("position", 0)
                initInfos(DatabaseHandler(applicationContext).getcurrentlist(4)[position])
                name.text=DatabaseHandler(applicationContext).getcurrentlist(1)[position]
                artist.text=DatabaseHandler(applicationContext).getcurrentlist(2)[position]
                play_pause.setImageResource(R.drawable.ic_play_icon)
            }
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
        play_pause.isEnabled=true
        seekBar.setOnSeekBarChangeListener(this)
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
        }
        else {
            play_pause.setImageResource(R.drawable.ic_play_icon)
        }
    }
}
