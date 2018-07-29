package arunapps.com.sharemusix

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class ArtistCardAdapter internal constructor(val mContext: Context, val martistList: ArrayList<String>, val mpathlist: ArrayList<String>) : RecyclerView.Adapter<ArtistCardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView = LayoutInflater.from(parent.context).inflate(R.layout.layout_custom_card_artist, parent, false)
        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle!!.text=martistList[position]
        holder.mButton!!.setOnClickListener {
            showPopupMenuAlbum(holder.itemView, position)
        }
        holder.mcard!!.setOnClickListener{
            val i: Intent? = Intent(mContext, ArtistListActivity::class.java)
            i!!.putExtra("artist_name",martistList[position])
            mContext.startActivity(i)
        }
        holder.mcard!!.setOnLongClickListener {
            showPopupMenuAlbum(holder.itemView,position)
            true
        }
        BackgroundTask(mpathlist[position],holder).execute()
    }

    override fun getItemCount(): Int {
        return martistList.size
    }
    private fun showPopupMenuAlbum(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_all, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_play_next -> {
                    val db = DatabaseHandler(mContext)
                    val mypref = PreferenceManager.getDefaultSharedPreferences(mContext)
                    var current_position = mypref.getInt("position", 0)
                    val path = db.getArtistList(3,martistList[position])
                    val fname= db.getArtistList(0,martistList[position])
                    val artist= db.getArtistList(1,martistList[position])
                    val album = db.getArtistList(2,martistList[position])
                    for(i in 0 until path.size) {
                        DatabaseHandler(mContext).updatecurrent(current_position)
                        DatabaseHandler(mContext).insertcurrent_normal(current_position+1, fname[i], artist[i], album[i], path[i])
                        current_position++
                    }
                }
                R.id.action_add_queue -> {
                    val db = DatabaseHandler(mContext)
                    val path = db.getArtistList(3,martistList[position])
                    val fname= db.getArtistList(0,martistList[position])
                    val artist= db.getArtistList(1,martistList[position])
                    val album = db.getArtistList(2,martistList[position])
                    for(i in 0 until path.size)
                        DatabaseHandler(mContext).insertcurrent_normal(DatabaseHandler(mContext).getcurrentcount(),fname[i],artist[i],album[i],path[i])
                    Toast.makeText(mContext,"Added to Queue",Toast.LENGTH_SHORT).show()
                }
                R.id.action_add_playlist -> {
                    val builderSingle = AlertDialog.Builder(ContextThemeWrapper(mContext, R.style.myDialog))
                    builderSingle.setTitle("Choose Playlist")
                    val arrayAdapter = ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item,DatabaseHandler(mContext).getPlaylistMain(0))
                    builderSingle.setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                    builderSingle.setAdapter(arrayAdapter, DialogInterface.OnClickListener { dialog, which ->
                        val playlistname = arrayAdapter.getItem(which)
                        val db = DatabaseHandler(mContext)
                        val path = db.getArtistList(3,martistList[position])
                        val fname= db.getArtistList(0,martistList[position])
                        val artist= db.getArtistList(1,martistList[position])
                        val album = db.getArtistList(2,martistList[position])
                        for(i in 0 until path.size)
                            DatabaseHandler(mContext).insertplaylist(playlistname,fname[i],artist[i],album[i],path[i])
                        Toast.makeText(mContext,"Added to Playlist",Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    })
                    builderSingle.show()
                }
            }
            true
        }
        popup.show()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var mImage: ImageView? = itemView.findViewById<ImageView>(R.id.ivImage) as ImageView
        var mTitle: TextView? = itemView.findViewById<TextView>(R.id.tvTitle) as TextView
        var mButton: ImageButton? = itemView.findViewById<ImageButton>(R.id.imageButton) as ImageButton
        var mcard: CardView? = itemView.findViewById<CardView>(R.id.cardview) as CardView
    }
    class BackgroundTask (val bpath: String, val holder1: ViewHolder) : AsyncTask<Void, Void, String>() {
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
            holder1.mImage!!.setImageBitmap(songImage)
            holder1.mImage!!.scaleType=ImageView.ScaleType.FIT_XY
            //change to default picture when no image is obtained in bitmap-arun
            if(songImage==null){
                holder1.mImage!!.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }
}
