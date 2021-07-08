package com.user.kasironline.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.R
import com.user.kasironline.adapter.PagerAdapter
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.model.CostsModel
import com.user.kasironline.utils.Validation.Companion.validateFields
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_purchase.*
import kotlinx.android.synthetic.main.add_costs_dialog.view.*
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var alertDialog: AlertDialog? = null
    private lateinit var dialogView: View
    private var username: String? = null
    private var price: String? = null
    private var information: String? = null
    private var priceValue: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        username = SharedPrefManager.getInstance(this)?.getUsername.toString()


        val tabMenus = arrayOf(
            resources.getString(R.string.product_list),
            resources.getString(R.string.sales),
            resources.getString(R.string.purchase),
            resources.getString(R.string.other_costs)
        )

        val pageAdapter = PagerAdapter(this)

        view_pager.adapter = pageAdapter

        TabLayoutMediator(
            tabs,
            view_pager
        ) { tab, position ->
            tab.text = tabMenus[position]
        }.attach()

        tabs.tabGravity = TabLayout.GRAVITY_FILL
        tabs.tabMode = TabLayout.MODE_SCROLLABLE

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> fab_add.hide()
                    1 -> fab_add.hide()
                    2 -> {
                        fab_add.show()
                        fab_add.setOnClickListener {
                            startActivity(Intent(applicationContext, PurchaseActivity::class.java))
                        }
                    }
                    3 -> {
                        fab_add.show()
                        fab_add.setOnClickListener {
                            addCostDialog()
                        }
                    }
                }
            }

        })
    }

    @SuppressLint("InflateParams")
    private fun addCostDialog() {
        val builder = MaterialAlertDialogBuilder(this@MainActivity)
        dialogView = layoutInflater.inflate(
            R.layout.add_costs_dialog, null
        )

        bindProgressButton(dialogView.btn_add)
        dialogView.btn_add.attachTextChangeAnimator()

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

        dialogView.btn_add.setOnClickListener {
            information = dialogView.input_information.text.toString()
            price = dialogView.input_price.text.toString()

            if (validateFields(information.toString()) || validateFields(price.toString())) {
                Toast.makeText(this, "Tidak boleh ada data yang kosong", Toast.LENGTH_SHORT)
                    .show()
            } else {
                priceValue = dialogView.input_price.text.toString().replace(".", "").toInt()
                dialogView.btn_add.showProgress { progressColor = Color.WHITE }
                val data = CostsModel()
                data.information = information
                data.price = price
                data.priceValue = priceValue
                data.date = Calendar.getInstance().time

                val itemId = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(username.toString())
                    .collection("costs")
                    .document()
                    .id

                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(username.toString())
                    .collection("costs")
                    .document(itemId)
                    .set(data)
                    .addOnSuccessListener {
                        db.collection("payment")
                            .document(itemId)
                            .set(data)
                            .addOnSuccessListener {
                                saveOtherCost()
                            }.addOnFailureListener {
                                dialogView.btn_add.hideProgress(R.string.btn_add)
                                Toast.makeText(
                                    this,
                                    it.localizedMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }.addOnFailureListener {
                        dialogView.btn_add.hideProgress(R.string.btn_add)
                        Toast.makeText(
                            this,
                            it.localizedMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        builder.setView(dialogView)
        builder.setTitle("Tambah Pengeluaran")

        alertDialog = builder.create()
        alertDialog?.show()
    }

    private fun saveOtherCost(){
        val data = CostsModel()
        data.date = Calendar.getInstance().time
        data.information = information
        data.priceValue = priceValue

        val db = FirebaseFirestore.getInstance()
        db.collection("costs")
            .document()
            .set(data)
            .addOnSuccessListener {
                dialogView.btn_add.hideProgress(R.string.btn_add)
                Toast.makeText(
                    this,
                    "Berhasil ditambahkan",
                    Toast.LENGTH_SHORT
                ).show()
                alertDialog?.dismiss()
            }.addOnFailureListener {
                dialogView.btn_add.hideProgress(R.string.btn_add)
                Toast.makeText(
                    this,
                    it.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout_menu -> {
                val builder = this.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle("Konfirmasi")
                        .setMessage("Anda yakin ingin keluar?")
                        .setPositiveButton("Ya") { _, _ ->
                            SharedPrefManager.getInstance(this)?.logoutUser()
                            startActivity(Intent(applicationContext, LoginActivity::class.java))
                            finish()
                        }
                        .setNegativeButton("Tidak") { dialog, _ ->
                            dialog.dismiss()
                        }
                }
                val dialog = builder.create()
                dialog.show()
            }
            R.id.cart_menu -> {
                startActivity(Intent(this, CartActivity::class.java))
            }
        }
        return true
    }
}