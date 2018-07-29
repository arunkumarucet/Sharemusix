package arunapps.com.sharemusix

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.ArrayList

class PlaylistListAdapter(private val context: Context, internal var fname: ArrayList<String>, internal var artist: ArrayList<String>, internal var album: ArrayList<String>, internal var path: ArrayList<String>,internal var playlistname: String) : BaseAdapter() {
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
            vi = inflater!!.inflate(R.layout.layout_custom_list_playlist, parent, false)
        val textView = vi!!.findViewById<View>(R.id.name) as TextView
        val textView1 = vi.findViewById<View>(R.id.artist) as TextView
        val imageButton = vi.findViewById<View>(R.id.imageButton4) as ImageButton
        imageButton.setImageResource(R.drawable.ic_more)
        textView.text = fname[position]
        textView1.text = artist[position]
        imageButton.setOnClickListener{
            showPopupMenu(vi!!,position)
        }
        vi.setOnLongClickListener {
            showPopupMenu(vi!!,position)
            true
        }
        return vi
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_playlist, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_play_next -> {
                    val mypref = PreferenceManager.getDefaultSharedPreferences(context)
                    val current_position = mypref.getInt("position", 0)
                    DatabaseHandler(context).updatecurrent(current_position)
                    DatabaseHandler(context).insertcurrent_normal(current_position+1,fname[position],artist[position],album[position],path[position])
                }
                R.id.action_add_queue -> {
                    DatabaseHandler(context).insertcurrent_normal(DatabaseHandler(context).getcurrentcount(),fname[position],artist[position],album[position],path[position])
                    Toast.makeText(context,"Added to queue",Toast.LENGTH_SHORT).show()
                }
                R.id.action_remove_playlist -> {
                    DatabaseHandler(context).deletecurrentplaylist(playlistname,path[position])
                    (context as PlaylistListActivity).loadlistview(playlistname)
                    (context as PlaylistListActivity).justifyListViewHeightBasedOnChildren()
                }
                }
            true
        }
        popup.show()
    }
    companion object {
        private var inflater: LayoutInflater? = null
    }
}