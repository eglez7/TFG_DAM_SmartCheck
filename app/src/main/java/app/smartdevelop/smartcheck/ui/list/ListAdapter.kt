package app.smartdevelop.smartcheck.ui.list

import android.annotation.SuppressLint
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import app.smartdevelop.smartcheck.R
import app.smartdevelop.smartcheck.databinding.ViewListBinding
import app.smartdevelop.smartcheck.inflate
import app.smartdevelop.smartcheck.model.Checklists
import app.smartdevelop.smartcheck.model.Details
import app.smartdevelop.smartcheck.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
class ListAdapter(
    items: List<Checklists> = emptyList(),
    private val listener: (Checklists) -> Unit
) : RecyclerView.Adapter<ListAdapter.ViewHolder>()  {

    // CON ESTA LÍNEA NOS ASEGURAMOS DE SABER CUÁNDO HAY UN CAMBIO EN LA LISTA DE ITEMS
    var items: List<Checklists> by Delegates.observable(items){ _, _, _, ->
        notifyDataSetChanged()
    }

        // DEVUELVE EL NÚMERO DE items DE LA LISTA
    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = parent.inflate(R.layout.view_list)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { listener(item) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener {
        private val binding = ViewListBinding.bind(view)
        private lateinit var listNow: Checklists

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        fun bind(listItem: Checklists) {
            listNow=listItem
            with(binding) {
                itemList.text=listItem.name
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu.add(R.string.edit).setOnMenuItemClickListener(onEditMenu)
            menu.add(R.string.delete).setOnMenuItemClickListener(onDeleteMenu)
        }

        private val onEditMenu = MenuItem.OnMenuItemClickListener {
            val input = EditText(itemView.context)
            input.setText(binding.itemList.text)

            val dialog = AlertDialog.Builder(itemView.context)
                .setTitle(R.string.edit_checklist)
                .setView(input)
                .setPositiveButton(R.string.save) { _, _ ->
                    val updatedChecklistName = input.text.toString()
                    if (updatedChecklistName.isNotEmpty()) {
                        updateListActivity(updatedChecklistName)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .create()

            dialog.show()
            true
        }

        private val onDeleteMenu = MenuItem.OnMenuItemClickListener {

            val builder = AlertDialog.Builder(itemView.context)
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro de que quieres continuar?")
            builder.setPositiveButton("Sí") { dialog, which ->
                CoroutineScope(Dispatchers.IO).launch {
                    MainActivity.room.checklistsDao().delete(listNow)
                }
            }
            builder.setNegativeButton("No") { dialog, which ->
            }
            builder.show()
            true
        }

        private fun updateListActivity(updatedChecklistName: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val dependientes : List<Details> = MainActivity.room.detailsDao().getDetailsById(listNow.id)
                MainActivity.room.detailsDao().deleteAll(dependientes)
                MainActivity.room.checklistsDao().update(
                    Checklists(listNow.id, updatedChecklistName)
                )
            }
        }

    }

}