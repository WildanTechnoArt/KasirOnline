package com.user.kasironline.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.user.kasironline.R
import com.user.kasironline.model.PurchaseModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.purchase_item.view.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class PurchaseListAdapter(
    options: FirestoreRecyclerOptions<PurchaseModel>
) :
    FirestoreRecyclerAdapter<PurchaseModel, PurchaseListAdapter.ViewHolder>(options) {

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
            .inflate(R.layout.purchase_item, parent, false)

        return ViewHolder(view)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: PurchaseModel) {
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        val getName = item.name.toString()
        val getSupplierName = item.supplier.toString()
        val getSupplierPhone = item.phone.toString()
        val getDate = formatter.format(item.date)
        val getStock = item.stock
        val getPriceValue = item.priceValue
        val getInformation = item.information.toString()

        holder.apply {
            containerView.tv_product_name.text = getName
            containerView.tv_date.text = "Tanggal: $getDate"
            containerView.tv_supplier_name.text = "Pemasok: $getSupplierName ($getSupplierPhone)"

            if (getInformation == "null") {
                containerView.tv_information.text = "Keterangan: -"
            } else {
                containerView.tv_information.text = "Keterangan: $getInformation"
            }

            containerView.tv_product_name.isSelected = true
            containerView.tv_price.text = getPriceValue?.toLong().let { kursIndonesia?.format(it) }
            containerView.tv_stock_value.text = "Jumlah: $getStock"
        }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}