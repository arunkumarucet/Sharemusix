package arunapps.com.sharemusix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.widget.*
import java.util.ArrayList
import android.view.*
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager


class SongsAdapter(private val context: Context, internal var fname: ArrayList<String>, internal var artist: ArrayList<String>, internal var album: ArrayList<String>,internal var path: ArrayList<String>) : BaseAdapter() {

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return fname.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var vi = convertView
        if (convertView == null)
            vi = inflater!!.inflate(R.layout.layout_custom_list_song, parent, false)
        val textView = vi!!.findViewById<View>(R.id.name) as TextView
        val textView1 = vi.findViewById<View>(R.id.artist) as TextView
        val imageButton = vi.findViewById<View>(R.id.imageButton4) as ImageButton
        val imageview = vi.findViewById<View>(R.id.imageView) as ImageView
        imageButton.setImageResource(R.drawable.ic_more)
        textView.text = fname[position]
        textView1.text = artist[position]
        BackgroundTask(path[position],imageview)
        /**
        val mData = MediaMetadataRetriever()
        var songImage: Bitmap? = null
        try {
            mData.setDataSource(path[position])
            val art = mData.embeddedPicture
            songImage = BitmapFactory.decodeByteArray(art, 0, art.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        imageview.setImageBitmap(songImage)
        imageview.scaleType=ImageView.ScaleType.FIT_XY
        //change to default picture when no image is obtained in bitmap-arun
        if(songImage==null){
            imageview.setImageResource(R.drawable.ic_launcher_background)
        }
        **/
        imageButton.setOnClickListener{
            showPopupMenu(vi!!,position)
        }
        vi.setOnClickListener{
            DatabaseHandler(context).deletecurrent()
            DatabaseHandler(context).insertcurrent_normal(0,fname[position],artist[position],album[position],path[position])
            BackgroundTask_Database(context,position,fname,artist,album,path).execute()
            val mypref = PreferenceManager.getDefaultSharedPreferences(context)
            val prefsEditr = mypref.edit()
            prefsEditr.putInt("position",0)
            prefsEditr.apply()
            val serviceIntent = Intent(context, PlayMusic::class.java)
            serviceIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
                context.startService(serviceIntent)
            context.stopService(serviceIntent)
            serviceIntent.action = Constants.ACTION.STARTFOREGROUND_ACTION
            context.startService(serviceIntent)

        }
        vi.setOnLongClickListener{
            showPopupMenu(vi!!,position)
            true
        }
        return vi
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_all, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_play_next -> {
                    val mypref = PreferenceManager.getDefaultSharedPreferences(context)
                    val current_position = mypref.getInt("position", 0)
                    DatabaseHandler(context).updatecurrent(current_position)
                    DatabaseHandler(context).insertcurrent_normal(current_position + 1, fname[position], artist[position], album[position], path[position])
                }
                R.id.action_add_queue -> {
                    DatabaseHandler(context).insertcurrent_normal(DatabaseHandler(context).getcurrentcount(),fname[position],artist[position],album[position],path[position])
                    Toast.makeText(context,"Added to queue",Toast.LENGTH_SHORT).show()
                }
                R.id.action_add_playlist -> {
                    val builderSingle = AlertDialog.Builder(ContextThemeWrapper(context, R.style.myDialog))
                    builderSingle.setTitle("Choose Playlist")
                    val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.select_dialog_item,DatabaseHandler(context).getPlaylistMain(0))
                    builderSingle.setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                    builderSingle.setAdapter(arrayAdapter, DialogInterface.OnClickListener { dialog, which ->
                        val playlistname = arrayAdapter.getItem(which)
                        DatabaseHandler(context).insertplaylist(playlistname,fname[position],artist[position],album[position],path[position])
                        Toast.makeText(context,"Added to Playlist",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    })
                    builderSingle.show()
                }
            }
            true
        }
        popup.show()
    }
    companion object {
        private var inflater: LayoutInflater? = null
    }
    class BackgroundTask (val bpath: String, val vie: ImageView) : AsyncTask<Void, Void, String>() {
        val mData = MediaMetadataRetriever()
        var songImage: Bitmap? = null
        override fun doInBackground(vararg params: Void): String? {
            try {
                mData.setDataSource(bpath)
                val art = mData.embeddedPicture
                songImage = BitmapFactory.decodeByteArray(art, 0, art.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            vie.setImageBitmap(songImage)
            vie.scaleType=ImageView.ScaleType.FIT_XY
            //change to default picture when no image is obtained in bitmap-arun
            if(songImage==null){
                vie.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }
    class BackgroundTask_Database (val context: Context,val position: Int, val fname: ArrayList<String>, val artist: ArrayList<String>, val album: ArrayList<String>, val path: ArrayList<String>) : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void): String? {
            var i1 = 1
            for (i in position+1 until fname.size) {
                DatabaseHandler(context).insertcurrent_normal(i1, fname[i], artist[i], album[i], path[i])
                i1++
            }
            return null
        }
    }
}