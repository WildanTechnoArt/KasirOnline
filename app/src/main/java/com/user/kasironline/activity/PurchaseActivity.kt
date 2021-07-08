package com.user.kasironline.activity

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.R
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.model.CostsModel
import com.user.kasironline.model.PurchaseModel
import com.user.kasironline.model.SupplierModel
import com.user.kasironline.utils.Validation.Companion.validateFields
import kotlinx.android.synthetic.main.activity_purchase.*
import java.text.NumberFormat
import java.util.*

class PurchaseActivity : AppCompatActivity() {

    private var productName: String? = null
    private var price: String? = null
    private var total: String? = null
    private var supplierName: String? = null
    private var supplierPhone: String? = null
    private var information: String? = null
    private var priceValue: Int? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)
        init()
    }

    private fun init() {
        username = SharedPrefManager.getInstance(this)?.getUsername.toString()

        bindProgressButton(btn_save)
        btn_save.attachTextChangeAnimator()

        input_price.addTextChangedListener(object : TextWatcher {
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
                    input_price.removeTextChangedListener(this)
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
                    input_price.setText(clean)
                    input_price.setSelection(clean.length)
                    input_price.addTextChangedListener(this)
                }
            }
        })

        btn_save.setOnClickListener {
            productName = input_product.text.toString()
            price = input_price.text.toString()
            total = input_total_product.text.toString()
            supplierName = input_supplier_name.text.toString()
            supplierPhone = input_phone.text.toString()
            information = input_information.text.toString()

            if (validateFields(productName.toString()) || validateFields(price.toString()) ||
                validateFields(total.toString()) || validateFields(supplierName.toString()) ||
                validateFields(supplierPhone.toString())
            ) {
                Toast.makeText(
                    this, "Tidak boleh ada data yang kosong",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                btn_save.showProgress { progressColor = Color.WHITE }
                priceValue = price.toString().replace(".", "").toInt()
                saveDataToUser()
            }
        }
    }

    private fun saveDataToUser() {
        val data = PurchaseModel()
        data.name = productName
        data.price = price
        data.priceValue = priceValue
        data.stock = total?.toInt()
        data.supplier = supplierName.toString()
        data.phone = supplierPhone.toString()
        data.information = information.toString()
        data.date = Calendar.getInstance().time

        val itemId = FirebaseFirestore.getInstance()
            .collection("users")
            .document(username.toString())
            .collection("purchase")
            .document()
            .id

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(username.toString())
            .collection("purchase")
            .document(itemId)
            .set(data)
            .addOnSuccessListener {
                saveDataToAdmin(data, itemId)
            }.addOnFailureListener {
                btn_save.hideProgress(R.string.btn_save)
                Toast.makeText(
                    this,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveDataToAdmin(data: PurchaseModel, itemId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("purchase")
            .document(itemId)
            .set(data)
            .addOnSuccessListener {
                savePayment()
            }.addOnFailureListener {
                btn_save.hideProgress(R.string.btn_save)
                Toast.makeText(
                    this,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun savePayment(){
        val data = CostsModel()
        data.date = Calendar.getInstance().time
        data.information = information
        data.priceValue = priceValue

        val db = FirebaseFirestore.getInstance()
        db.collection("payment")
            .document()
            .set(data)
            .addOnSuccessListener {
                saveDataSupplier()
            }.addOnFailureListener {
                btn_save.hideProgress(R.string.btn_save)
                Toast.makeText(
                    this,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveDataSupplier() {
        val data = SupplierModel()
        data.supplier = supplierName.toString()
        data.phone = supplierPhone.toString()

        val db = FirebaseFirestore.getInstance()
        db.collection("supplier")
            .document()
            .set(data)
            .addOnSuccessListener {
                btn_save.hideProgress(R.string.btn_save)
                Toast.makeText(
                    this,
                    "Data pembelian produk berhasil ditambahkan",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }.addOnFailureListener {
                btn_save.hideProgress(R.string.btn_save)
                Toast.makeText(
                    this,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}