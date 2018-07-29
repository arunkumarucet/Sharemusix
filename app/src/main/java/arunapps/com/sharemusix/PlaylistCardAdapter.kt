package arunapps.com.sharemusix

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v7.widget.CardView
import android.widget.*


class PlaylistCardAdapter internal constructor(val mContext: Context, val mplaylist: ArrayList<String>) : RecyclerView.Adapter<PlaylistCardAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val mView = LayoutInflater.from(parent.context).inflate(R.layout.layout_custom_card_playlist, parent, false)
        return ViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mTitle1!!.text=mplaylist[position]
        holder.mImage1!!.setImageResource(R.drawable.ic_launcher_background)
        holder.mButton1!!.setOnClickListener {
            showPopupMenuAlbum(holder.itemView, position)
        }
        holder.card1!!.setOnClickListener{
            val i:Intent? =Intent(mContext, PlaylistListActivity::class.java)
            i!!.putExtra("playlist_name",mplaylist[position])
            mContext.startActivity(i)
        }
        holder.card1!!.setOnLongClickListener {
            showPopupMenuAlbum(holder.itemView,position)
            true
        }
    }

    override fun getItemCount(): Int {
        return mplaylist.size
    }
    private fun showPopupMenuAlbum(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_playlist_caard, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_play_next -> {
                    val db = DatabaseHandler(mContext)
                    val mypref = PreferenceManager.getDefaultSharedPreferences(mContext)
                    var current_position = mypref.getInt("position", 0)
                    val path = db.getPlayList(4,mplaylist[position])
                    val fname= db.getPlayList(1,mplaylist[position])
                    val artist= db.getPlayList(2,mplaylist[position])
                    val album = db.getPlayList(3,mplaylist[position])
                    for(i in 0 until path.size) {
                        DatabaseHandler(mContext).updatecurrent(current_position)
                        DatabaseHandler(mContext).insertcurrent_normal(current_position+1, fname[i], artist[i], album[i], path[i])
                        current_position++
                    }
                }
                R.id.action_add_queue -> {
                    val db = DatabaseHandler(mContext)
                    val path = db.getPlayList(4,mplaylist[position])
                    val fname= db.getPlayList(1,mplaylist[position])
                    val artist= db.getPlayList(2,mplaylist[position])
                    val album = db.getPlayList(3,mplaylist[position])
                    for(i in 0 until path.size)
                        DatabaseHandler(mContext).insertcurrent_normal(DatabaseHandler(mContext).getcurrentcount(),fname[i],artist[i],album[i],path[i])
                    Toast.makeText(mContext,"Added to Queue",Toast.LENGTH_SHORT).show()
                }
                R.id.action_remove -> {
                    DatabaseHandler(mContext).deletetplaylist(mplaylist[position])
                    DatabaseHandler(mContext).deletetplaylistmain(mplaylist[position])
                    Toast.makeText(mContext,"Playlist Deleted",Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        popup.show()
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var mImage1:ImageView? = itemView.findViewById<ImageView>(R.id.ivImage_playlist) as ImageView
        var mTitle1:TextView? = itemView.findViewById<TextView>(R.id.tvTitle_playlist) as TextView
        var mButton1: ImageButton? = itemView.findViewById<ImageButton>(R.id.imageButton_playlist) as ImageButton
        var card1: CardView? = itemView.findViewById<CardView>(R.id.cardview_playlist) as CardView
    }
}