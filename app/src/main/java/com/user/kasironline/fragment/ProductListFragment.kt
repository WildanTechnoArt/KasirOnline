package com.user.kasironline.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.R
import com.user.kasironline.adapter.ProductListAdapter
import com.user.kasironline.model.ProductModel
import com.user.kasironline.utils.Validation
import com.user.kasironline.view.ProductListView
import kotlinx.android.synthetic.main.edit_product_dialog.view.*
import kotlinx.android.synthetic.main.fragment_tab_item.*
import java.text.NumberFormat
import java.util.*
import kotlin.collections.HashMap

class ProductListFragment : Fragment(), ProductListView {

    private var alertDialog: AlertDialog? = null
    private lateinit var dialogView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tab_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepare()
        checkData()
    }

    private fun prepare() {
        rv_item?.layoutManager = GridLayoutManager(context, 2)
        rv_item?.setHasFixedSize(true)

        swipe_refresh?.setOnRefreshListener {
            checkData()
        }
    }

    private fun requestData() {
        val query = FirebaseFirestore.getInstance()
            .collection("product")

        val options = FirestoreRecyclerOptions.Builder<ProductModel>()
            .setQuery(query, ProductModel::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = ProductListAdapter(options, this)
        rv_item?.adapter = adapter
    }

    private fun checkData() {
        swipe_refresh?.isRefreshing = true

        val db = FirebaseFirestore.getInstance()
        db.collection("product")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot?.isEmpty == true) {
                    tv_not_data?.visibility = View.VISIBLE
                    rv_item?.visibility = View.GONE
                } else {
                    tv_not_data?.visibility = View.GONE
                    rv_item?.visibility = View.VISIBLE
                    requestData()
                }

                swipe_refresh?.isRefreshing = false
            }
    }

    @SuppressLint("InflateParams")
    override fun onEditProductDialog(id: String, data: ProductModel) {
        val builder = context?.let { MaterialAlertDialogBuilder(it) }
        dialogView = (context as AppCompatActivity).layoutInflater.inflate(
            R.layout.edit_product_dialog, null
        )

        bindProgressButton(dialogView.btn_edit)
        dialogView.btn_edit.attachTextChangeAnimator()
        dialogView.btn_edit.text = getString(R.string.btn_edit)

        dialogView.input_discount.setText(data.discount.toString())

        dialogView.input_price.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int,
                after: Int
            ) {

            }

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    dialogView.input_price.removeTextChangedListener(this)
                    val local = Locale("in", "ID")

                    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    val replaceable = java.lang.String.format(
                        "[Rp,.\\s]",
                        NumberFormat.getCurrencyInstance().currency
                            .getSymbol(local)
                    )
                    val cleanString = s.toString().replace(
                        replaceable.toRegex(),
                        ""
                    )
                    val parsed: Double = try {
                        cleanString.toDouble()
                    } catch (e: NumberFormatException) {
                        0.00
                    }
                    val formatter: NumberFormat = NumberFormat
                        .getCurrencyInstance(local)
                    formatter.maximumFractionDigits = 0
                    formatter.isParseIntegerOnly = true
                    val formatted: String = formatter.format(parsed)

                    val replace = java.lang.String.format(
                        "[Rp\\s]",
                        NumberFormat.getCurrencyInstance(local)
                    )
                    val clean = formatted.replace(replace.toRegex(), "")
                    current = formatted
                    dialogView.input_price.setText(clean)
                    dialogView.input_price.setSelection(clean.length)
                    dialogView.input_price.addTextChangedListener(this)
                }
            }
        })

        dialogView.btn_edit.setOnClickListener {
            val price = dialogView.input_price.text.toString()
            val discount = dialogView.input_discount.text.toString()

            if (Validation.validateFields(price) || Validation.validateFields(discount)) {
                Toast.makeText(context, "Tidak boleh ada data yang kosong", Toast.LENGTH_SHORT)
                    .show()
            } else {
                if (discount.toInt() > 100) {
                    Toast.makeText(
                        context,
                        "Diskon tidak boleh lebih dari 100%",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    val priceValue = dialogView.input_price.text.toString().replace(".", "").toInt()

                    dialogView.btn_edit.showProgress { progressColor = Color.WHITE }

                    val product = HashMap<String, Any>()
                    product["price"] = price
                    product["priceValue"] = priceValue
                    product["discount"] = discount.toInt()

                    val db = FirebaseFirestore.getInstance()
                    db.collection("product")
                        .document(id)
                        .update(product)
                        .addOnSuccessListener {
                            dialogView.btn_edit.hideProgress(R.string.btn_edit)
                            Toast.makeText(
                                context,
                                "Produk berhasil diedit",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            alertDialog?.dismiss()
                        }.addOnFailureListener {
                            dialogView.btn_edit.hideProgress(R.string.btn_edit)
                            Toast.makeText(
                                context,
                                it.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        }

        builder?.setView(dialogView)
        builder?.setTitle("Edit Barang")

        alertDialog = builder?.create()
        alertDialog?.show()
    }
}