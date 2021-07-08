package com.user.kasironline.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.user.kasironline.R
import com.user.kasironline.model.CartModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.cart_item.view.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class OrderListAdapter(
    options: FirestoreRecyclerOptions<CartModel>,
) :
    FirestoreRecyclerAdapter<CartModel, OrderListAdapter.ViewHolder>(options) {

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
            .inflate(R.layout.cart_item, parent, false)

        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: CartModel) {
        val getName = item.name.toString()
        val getPrice = item.priceValue
        val getAmount = item.amount

        holder.apply {
            containerView.tv_product_name.text = getName
            containerView.btn_delete.visibility = GONE
            containerView.tv_product_price.text =
                getPrice?.toLong()?.let { kursIndonesia?.format(it) }
            containerView.tv_product_count.text = "Qty: $getAmount"
        }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}