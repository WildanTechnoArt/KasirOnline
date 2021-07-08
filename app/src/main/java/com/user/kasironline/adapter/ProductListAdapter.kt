package com.user.kasironline.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.GlideApp
import com.user.kasironline.R
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.model.CartModel
import com.user.kasironline.model.ProductModel
import com.user.kasironline.view.ProductListView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.product_item.view.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class ProductListAdapter(
    options: FirestoreRecyclerOptions<ProductModel>,
    private val listener: ProductListView
) :
    FirestoreRecyclerAdapter<ProductModel, ProductListAdapter.ViewHolder>(options) {

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
            .inflate(R.layout.product_item, parent, false)

        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: ProductModel) {
        val getKey = snapshots.getSnapshot(position).id
        val getContext = holder.itemView.context
        val username = SharedPrefManager.getInstance(getContext)?.getUsername.toString()
        val getImage = item.image.toString()
        val getName = item.name.toString()
        val getStock = item.stock
        val getPriceValue = item.priceValue
        val getDiscount: Double = item.discount?.toDouble() ?: 0.0
        val getPrice = item.priceValue?.times((getDiscount.div(100.0)))?.let {
            item.priceValue?.minus(it)
        }

        holder.apply {
            GlideApp.with(getContext)
                .load(getImage)
                .into(containerView.img_product)

            var mProductCount = Integer.parseInt(containerView.tv_stock.text.toString())

            containerView.tv_product_name.text = getName
            containerView.tv_product_name.isSelected = true
            containerView.tv_price.text = getPrice?.toLong()?.let { kursIndonesia?.format(it) }

            if (getDiscount > 0.0) {
                containerView.ly_discount.visibility = View.VISIBLE
                containerView.discount.text = "${getDiscount.toInt()}%"
            } else {
                containerView.ly_discount.visibility = View.GONE
            }

            containerView.tv_stock_value.text = "Stok: $getStock"

            containerView.btn_min.setOnClickListener {
                containerView.btn_plus.isEnabled = true
                mProductCount -= 1
                containerView.tv_stock.text = mProductCount.toString()

                if (mProductCount > 0) {
                    containerView.btn_min.isEnabled = true
                    containerView.btn_confirmation.isEnabled = true
                } else {
                    containerView.btn_min.isEnabled = false
                    containerView.btn_confirmation.isEnabled = false
                }
            }

            containerView.btn_plus.setOnClickListener {
                mProductCount += 1
                containerView.tv_stock.text = mProductCount.toString()
                if (mProductCount > 0) {
                    if (mProductCount >= getStock ?: 0) {
                        containerView.btn_plus.isEnabled = false
                    }
                    containerView.btn_min.isEnabled = true
                    containerView.btn_confirmation.isEnabled = true
                } else {
                    containerView.btn_min.isEnabled = false
                    containerView.btn_confirmation.isEnabled = false
                }
            }

            (getContext as AppCompatActivity).bindProgressButton(containerView.btn_confirmation)
            containerView.btn_confirmation.attachTextChangeAnimator()

            containerView.btn_confirmation.setOnClickListener {
                containerView.btn_confirmation.showProgress { progressColor = Color.WHITE }

                val data = CartModel()
                data.amount = mProductCount
                data.discount = getDiscount.toInt()
                data.name = getName
                data.price = getPriceValue
                data.priceValue = getPriceValue?.times(mProductCount)

                val db = FirebaseFirestore.getInstance()
                val db2 = FirebaseFirestore.getInstance()
                db.collection("payment")
                    .document(username)
                    .collection("cart")
                    .document(getKey)
                    .set(data)
                    .addOnSuccessListener {
                        db2.collection("product")
                            .document(getKey)
                            .update("stock", FieldValue.increment(-mProductCount.toLong()))
                            .addOnSuccessListener {
                                containerView.btn_confirmation.hideProgress(
                                    getContext.resources.getString(
                                        R.string.btn_add
                                    )
                                )
                                Toast.makeText(
                                    getContext,
                                    "Berhasil ditamhahkan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener {
                                containerView.btn_confirmation.hideProgress(
                                    getContext.resources.getString(
                                        R.string.btn_add
                                    )
                                )
                                Toast.makeText(
                                    getContext,
                                    it.localizedMessage?.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }.addOnFailureListener {
                        containerView.btn_confirmation.hideProgress(getContext.resources.getString(R.string.btn_add))
                        Toast.makeText(
                            getContext,
                            it.localizedMessage?.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            containerView.img_menu.setOnClickListener {
                val popupMenu = PopupMenu(it.context, it)
                popupMenu.setOnMenuItemClickListener(object :
                    android.widget.PopupMenu.OnMenuItemClickListener,
                    PopupMenu.OnMenuItemClickListener {
                    override fun onMenuItemClick(p0: MenuItem?): Boolean {
                        when (p0?.itemId) {
                            R.id.menu_edit -> {
                                val data = ProductModel()
                                data.priceValue = item.priceValue
                                data.image = item.image.toString()
                                data.stock = item.stock
                                data.discount = item.discount
                                data.name = item.name.toString()

                                listener.onEditProductDialog(
                                    snapshots.getSnapshot(position).id,
                                    data
                                )
                            }
                        }
                        return true
                    }
                })
                popupMenu.inflate(R.menu.product_list)
                popupMenu.show()
            }
        }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}