package com.user.kasironline.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.user.kasironline.R
import com.user.kasironline.model.CostsModel
import com.user.kasironline.view.CostListView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.costs_item.view.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class CostsListAdapter(
    options: FirestoreRecyclerOptions<CostsModel>,
    private val listener: CostListView
) :
    FirestoreRecyclerAdapter<CostsModel, CostsListAdapter.ViewHolder>(options) {

    private var kursIndonesia: DecimalFormat? = null

    init {
        kursIndonesia = DecimalFormat.getCurrencyInstance() as DecimalFormat
        val formatRp = DecimalFormatSymbols()

        formatRp.currencySymbol = "Rp. "
        formatRp.monetaryDecimalSeparator = ','
        formatRp.groupingSeparator = '.'
        kursIndonesia?.decimalFormatSymbols = formatRp
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.costs_item, parent, false)

        return ViewHolder(view)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: CostsModel) {
        val mContext = holder.containerView.context
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        val getInformation = item.information.toString()
        val getDate = formatter.format(item.date)
        val getPriceValue = item.priceValue

        holder.apply {
            containerView.tv_information.text = "Keterangan: $getInformation"
            containerView.tv_date.text = "Tanggal: $getDate"
            containerView.tv_price.text = getPriceValue?.toLong().let { kursIndonesia?.format(it) }
            containerView.img_more.setOnClickListener { it1 ->
                val popupMenu = PopupMenu(it1.context, it1)
                popupMenu.setOnMenuItemClickListener(object :
                    android.widget.PopupMenu.OnMenuItemClickListener,
                    PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(p0: MenuItem?): Boolean {
                        when (p0?.itemId) {
                            R.id.menu_delete -> {
                                val builder = mContext.let { it1 ->
                                    MaterialAlertDialogBuilder(it1)
                                        .setTitle("Konfirmasi")
                                        .setMessage("Anda yakin ingin menghapusnya?")
                                        .setPositiveButton("Ya") { _, _ ->
                                            listener.onDelete(snapshots.getSnapshot(position).id)
                                        }
                                        .setNegativeButton("Tidak") { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                }
                                val dialog = builder.create()
                                dialog.show()
                            }
                        }
                        return true
                    }
                })
                popupMenu.inflate(R.menu.cost_list)
                popupMenu.show()
            }
        }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}