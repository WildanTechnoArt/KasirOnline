package com.user.kasironline.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import com.itextpdf.text.pdf.draw.VerticalPositionMark
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.user.kasironline.R
import com.user.kasironline.adapter.PdfDocumentAdapter
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.model.CartModel
import com.user.kasironline.model.CostsModel
import com.user.kasironline.model.CustomerModel
import com.user.kasironline.model.SalesModel
import com.user.kasironline.utils.Common
import com.user.kasironline.utils.Validation.Companion.validateFields
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_cart.*
import kotlinx.android.synthetic.main.add_customer_dialog.view.*
import kotlinx.android.synthetic.main.cart_item.view.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class CartActivity : AppCompatActivity() {

    private var username: String? = null
    private var customerName: String? = null
    private var customerInformation: String? = null
    private var date: String? = null
    private var customerPhone: String? = null
    private var kursIndonesia: DecimalFormat? = null
    private var alertDialog: AlertDialog? = null
    private lateinit var dialogView: View
    private var totalPrice = 0.0
    private var totalCount = 0
    private val orderList = ArrayList<CartModel>()
    private val listKey = ArrayList<String>()
    private lateinit var adapter: FirestoreRecyclerAdapter<CartModel, CartActivity.ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)
        prepare()
        checkData()
    }

    private fun prepare() {
        username = SharedPrefManager.getInstance(this)?.getUsername.toString()

        kursIndonesia = DecimalFormat.getCurrencyInstance() as DecimalFormat
        val formatRp = DecimalFormatSymbols()

        formatRp.currencySymbol = "Rp. "
        formatRp.monetaryDecimalSeparator = ','
        formatRp.groupingSeparator = '.'
        kursIndonesia?.decimalFormatSymbols = formatRp

        rv_item.layoutManager = LinearLayoutManager(this)
        rv_item.setHasFixedSize(true)

        swipe_refresh.setOnRefreshListener {
            checkData()
        }

        btn_confirmation.setOnClickListener {
            addCustomerDialog()
        }
    }

    @SuppressLint("InflateParams")
    private fun addCustomerDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        dialogView = layoutInflater.inflate(
            R.layout.add_customer_dialog, null
        )

        bindProgressButton(dialogView.btn_confirmation)
        dialogView.btn_confirmation.attachTextChangeAnimator()

        dialogView.btn_confirmation.setOnClickListener {
            customerName = dialogView.input_username.text.toString()
            customerPhone = dialogView.input_phone.text.toString()
            customerInformation = dialogView.input_information.text.toString()

            if (validateFields(customerName.toString()) || validateFields(customerPhone.toString())
                || validateFields(customerInformation.toString())
            ) {
                Toast.makeText(this, "Tidak boleh ada data yang kosong", Toast.LENGTH_SHORT)
                    .show()
            } else {
                alertDialog?.setCancelable(false)
                dialogView.btn_confirmation.showProgress { progressColor = Color.WHITE }
                getPermission()
            }
        }

        builder.setView(dialogView)
        builder.setTitle("Konfirmasi Pelanggan")

        alertDialog = builder.create()
        alertDialog?.show()
    }

    private fun saveCustomer() {
        val customer = CustomerModel()
        customer.name = customerName
        customer.phone = customerPhone

        val db = FirebaseFirestore.getInstance()
        db.collection("customer")
            .document(customerPhone.toString())
            .set(customer)
            .addOnSuccessListener {
                saveSalesForAdmin()
            }.addOnFailureListener {
                dialogView.btn_confirmation.hideProgress(R.string.btn_invoice)
                Toast.makeText(
                    this,
                    it.localizedMessage?.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    saveCustomer()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    alertDialog?.dismiss()
                    dialogView.btn_confirmation.hideProgress(R.string.btn_invoice)
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            }).check()
    }

    private fun saveSalesForAdmin() {
        val db = FirebaseFirestore.getInstance()
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        date = formatter.format(Calendar.getInstance().time)

        val data = SalesModel()
        data.name = customerName
        data.phone = customerPhone
        data.information = customerInformation
        data.totalCount = totalCount.toString()
        data.totalPrice = totalPrice.toLong().let { kursIndonesia?.format(it) }
        data.datetime = Calendar.getInstance().time

        db.collection("sales")
            .document(customerPhone.toString())
            .set(data)
            .addOnSuccessListener {
                saveSalesForUser(customerPhone.toString(), data)
            }.addOnFailureListener {
                dialogView.btn_confirmation.hideProgress(R.string.btn_invoice)
                Toast.makeText(
                    this,
                    it.localizedMessage?.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveSalesForUser(phone: String, data: SalesModel) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(username.toString())
            .collection("sales")
            .document(phone)
            .set(data)
            .addOnSuccessListener {
                saveReception(phone)
            }.addOnFailureListener {
                dialogView.btn_confirmation.hideProgress(R.string.btn_invoice)
                Toast.makeText(
                    this,
                    it.localizedMessage?.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveReception(phone: String) {
        val data = CostsModel()
        data.date = Calendar.getInstance().time
        data.information = customerInformation
        data.priceValue = totalPrice.toInt()

        val db = FirebaseFirestore.getInstance()

        db.collection("reception")
            .document()
            .set(data)
            .addOnSuccessListener {
                saveAllOrder(phone)
            }.addOnFailureListener {
                dialogView.btn_confirmation.hideProgress(R.string.btn_invoice)
                Toast.makeText(
                    this,
                    it.localizedMessage?.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveAllOrder(phone: String) {
        for (a in 0 until adapter.itemCount) {
            val data = CartModel()
            data.name = orderList[a].name
            data.priceValue = orderList[a].priceValue
            data.price = orderList[a].price
            data.discount = orderList[a].discount
            data.amount = orderList[a].amount

            val db = FirebaseFirestore.getInstance()
            db.collection("sales")
                .document(phone)
                .collection("order")
                .document()
                .set(data)
                .addOnSuccessListener {
                    if (a >= (adapter.itemCount - 1)) {
                        clearOrder()
                    }
                }
                .addOnFailureListener { e: java.lang.Exception ->
                    Toast.makeText(
                        applicationContext,
                        e.localizedMessage, Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun createPdfFIle(path: String) {
        if (File(path).exists())
            File(path).delete()
        try {
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(path))
            document.open()

            // Document Setting
            document.pageSize = PageSize.A4
            document.addCreationDate()
            document.addAuthor("Royal Truss")
            document.addCreator("Royal Truss")

            // Font Setting
            val colorAccent = BaseColor(0, 153, 204, 255)
            val fontSize = 26.0f
            val valueFontSize = 26.0f

            // Custom Font
            val fontName =
                BaseFont.createFont("assets/fonts/ArialCE.ttf", "UTF-8", BaseFont.EMBEDDED)

            val orderNumberValueFont = Font(fontName, valueFontSize, Font.NORMAL, BaseColor.BLACK)
            val orderNumberFont = Font(fontName, fontSize, Font.NORMAL, colorAccent)
            val titleFont = Font(fontName, 36.0f, Font.NORMAL, BaseColor.BLACK)
            val productFont = Font(fontName, fontSize, Font.NORMAL, BaseColor.BLACK)

            addNewItem(document, "Royal Truss", Element.ALIGN_CENTER, titleFont)
            addLineSpace(document)
            addNewItem(
                document,
                "Menjual: Baja Ringan, Atap Metal dan Plafon",
                Element.ALIGN_LEFT,
                orderNumberValueFont
            )
            addLineSpace(document)
            addNewItem(
                document,
                "Alamat: Ruko Pertokoan, Jl. Lkr, Sel. No. 10, Dayeuhluhur, Kec. Warudoyong, Kota Sukabumi",
                Element.ALIGN_LEFT,
                orderNumberValueFont
            )

            addLineSeparator(document)

            // Create Title of Document
            addNewItem(document, "Detail Penjualan", Element.ALIGN_CENTER, titleFont)

            // Add More
            addNewItem(document, "Nama Pelanggan", Element.ALIGN_LEFT, orderNumberFont)
            addNewItem(document, customerName.toString(), Element.ALIGN_LEFT, orderNumberValueFont)

            addLineSeparator(document)

            addNewItem(document, "No. HP", Element.ALIGN_LEFT, orderNumberFont)
            addNewItem(document, customerPhone.toString(), Element.ALIGN_LEFT, orderNumberValueFont)

            addLineSeparator(document)

            addNewItem(document, "Tanggal", Element.ALIGN_LEFT, orderNumberFont)
            addNewItem(document, date.toString(), Element.ALIGN_LEFT, orderNumberValueFont)

            addLineSeparator(document)

            addNewItem(document, "Keterangan", Element.ALIGN_LEFT, orderNumberFont)
            addNewItem(
                document,
                customerInformation.toString(),
                Element.ALIGN_LEFT,
                orderNumberValueFont
            )

            addLineSeparator(document)

            // Add Product order detail
            addLineSpace(document)
            addNewItem(document, "Detail Produk", Element.ALIGN_CENTER, titleFont)

            for (data in orderList) {
                // Item
                if ((data.discount ?: 0) > 0) {
                    addNewItemWithLeftAndRight(
                        document,
                        data.name.toString(),
                        "Diskon ${data.discount}%",
                        productFont,
                        orderNumberValueFont
                    )
                } else {
                    addNewItemWithLeftAndRight(
                        document,
                        data.name.toString(),
                        "",
                        productFont,
                        orderNumberValueFont
                    )
                }

                addNewItemWithLeftAndRight(
                    document,
                    "${data.price?.toLong()?.let { kursIndonesia?.format(it) }} x ${data.amount}",
                    "${data.priceValue?.toLong()?.let { kursIndonesia?.format(it) }}",
                    productFont,
                    orderNumberValueFont
                )
                addLineSeparator(document)
            }

            // Total
            addLineSpace(document)
            addLineSpace(document)

            addNewItemWithLeftAndRight(
                document,
                "Total",
                "${totalPrice.toLong().let { kursIndonesia?.format(it) }}",
                productFont,
                orderNumberValueFont
            )
            document.close()

            printPDF()
            if (alertDialog != null) {
                alertDialog?.dismiss()
            }

            tv_total_order.text = resources.getString(R.string.no_data)
            tv_total_price.text = resources.getString(R.string.no_data)
            btn_confirmation.isEnabled = false
        } catch (ex: FileNotFoundException) {
            ex.printStackTrace()
        } catch (ex: DocumentException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun printPDF() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        try {
            val printDocumentAdapter =
                PdfDocumentAdapter(Common.getAppPath(this@CartActivity) + "Invoice.pdf")
            printManager.print("Document", printDocumentAdapter, PrintAttributes.Builder().build())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun addNewItemWithLeftAndRight(
        document: Document,
        textLeft: String,
        textRight: String,
        textLeftFont: Font,
        textRightFont: Font
    ) {
        val chunkTextLeft = Chunk(textLeft, textLeftFont)
        val chunkTextRight = Chunk(textRight, textRightFont)
        val p = Paragraph(chunkTextLeft)
        p.add(Chunk(VerticalPositionMark()))
        p.add(chunkTextRight)
        document.add(p)
    }

    @Throws(DocumentException::class)
    private fun addLineSeparator(document: Document) {
        val lineSeparator = LineSeparator()
        lineSeparator.lineColor = BaseColor(0, 0, 68)
        addLineSpace(document)
        document.add(Chunk(lineSeparator))
        addLineSpace(document)
    }

    @Throws(DocumentException::class)
    private fun addLineSpace(document: Document) {
        document.add(Paragraph(""))
    }

    @Throws(DocumentException::class)
    private fun addNewItem(
        document: Document,
        text: String,
        align: Int,
        font: Font
    ) {
        val chunk = Chunk(text, font)
        val paragraph = Paragraph(chunk)
        paragraph.alignment = align
        document.add(paragraph)
    }

    private fun clearOrder() {
        for (a in 0 until adapter.itemCount) {
            val db = FirebaseFirestore.getInstance()
            db.collection("payment")
                .document(username.toString())
                .collection("cart")
                .document(listKey[a])
                .delete()
                .addOnSuccessListener {
                    if (a >= (adapter.itemCount - 1)) {
                        createPdfFIle(
                            Common.getAppPath(this@CartActivity) + "Invoice.pdf"
                        )
                    }
                }
                .addOnFailureListener { e: java.lang.Exception ->
                    Toast.makeText(
                        applicationContext,
                        e.localizedMessage, Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun getAllOrder() {
        try {
            val rootRef = FirebaseFirestore.getInstance()
            val subjectsRef = rootRef.collection("payment").document(username.toString())
                .collection("cart")

            subjectsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val data = CartModel()
                        val name = document.getString("name")
                        val priceValue = document.getLong("priceValue")
                        val price = document.getLong("price")
                        val discount = document.getLong("discount")
                        val amount = document.getLong("amount")
                        data.name = name
                        data.priceValue = priceValue?.toInt()
                        data.price = price?.toInt()
                        data.discount = discount?.toInt()
                        data.amount = amount?.toInt()
                        orderList.add(data)
                    }
                    swipe_refresh?.isRefreshing = false
                    btn_confirmation.isEnabled = true
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            swipe_refresh?.isRefreshing = false
        }
    }

    private fun requestData() {
        swipe_refresh.isRefreshing = true

        val query = FirebaseFirestore.getInstance()
            .collection("payment")
            .document(username.toString())
            .collection("cart")

        val options = FirestoreRecyclerOptions.Builder<CartModel>()
            .setQuery(query, CartModel::class.java)
            .setLifecycleOwner(this)
            .build()

        adapter = object : FirestoreRecyclerAdapter<CartModel, CartActivity.ViewHolder>(options) {
            override fun onDataChanged() {
                super.onDataChanged()
                swipe_refresh.isRefreshing = false
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): CartActivity.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.cart_item, parent, false)

                return ViewHolder(view)
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(
                holder: CartActivity.ViewHolder,
                position: Int,
                item: CartModel
            ) {
                val getContext = holder.itemView.context
                val snapshotId = snapshots.getSnapshot(position).id
                val getName = item.name.toString()
                val getDiscount: Double = item.discount?.toDouble() ?: 0.0
                val getPrice = item.priceValue?.times((getDiscount.div(100.0)))?.let {
                    item.priceValue?.minus(it)
                }
                val getAmount = item.amount
                listKey.add(snapshots.getSnapshot(position).id)

                holder.apply {
                    containerView.tv_product_name.text = getName

                    if (getDiscount > 0.0) {
                        containerView.tv_product_price.text =
                            "${
                                getPrice?.toLong()?.let { kursIndonesia?.format(it) }
                            } (Diskon ${getDiscount.toInt()}%)"
                    } else {
                        containerView.tv_product_price.text =
                            getPrice?.toLong()?.let { kursIndonesia?.format(it) }
                    }

                    containerView.tv_product_count.text = "Qty: $getAmount"

                    totalCount += getAmount ?: 0
                    totalPrice += getPrice ?: 0.0

                    countTotal()

                    containerView.btn_delete.setOnClickListener {
                        val builder = getContext.let {
                            MaterialAlertDialogBuilder(it)
                                .setTitle("Konfirmasi")
                                .setMessage("Anda yakin ingin menghapusnya?")
                                .setPositiveButton("Ya") { _, _ ->
                                    changeStock(snapshotId, getAmount)
                                }
                                .setNegativeButton("Tidak") { dialog, _ ->
                                    dialog.dismiss()
                                }
                        }
                        val dialog = builder.create()
                        dialog.show()
                    }
                }
            }

        }
        rv_item?.adapter = adapter
    }

    private fun deleteItemCart(itemId: String) {
        totalPrice = 0.0
        totalCount = 0

        val db = FirebaseFirestore.getInstance()
        db.collection("payment")
            .document(username.toString())
            .collection("cart")
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                if (adapter.itemCount == 0) {
                    btn_confirmation.isEnabled = false
                    tv_total_order.text = getString(R.string.nodata)
                    tv_total_price.text = getString(R.string.nodata)
                }
                swipe_refresh.isRefreshing = false
            }
            .addOnFailureListener { e: Exception ->
                swipe_refresh.isRefreshing = false
                Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }

    private fun changeStock(itemId: String, count: Int?) {
        swipe_refresh.isRefreshing = true

        val db = FirebaseFirestore.getInstance()
        db.collection("product")
            .document(itemId)
            .update("stock", count?.toLong()?.let { FieldValue.increment(it) })
            .addOnSuccessListener {
                deleteItemCart(itemId)
            }
            .addOnFailureListener {
                swipe_refresh.isRefreshing = false
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkData() {
        totalPrice = 0.0
        totalCount = 0

        swipe_refresh?.isRefreshing = true

        val db = FirebaseFirestore.getInstance()
        db.collection("payment")
            .document(username.toString())
            .collection("cart")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot?.isEmpty == true) {
                    rv_item?.visibility = View.GONE
                } else {
                    rv_item?.visibility = View.VISIBLE
                    requestData()
                    getAllOrder()
                }

                swipe_refresh?.isRefreshing = false
            }
    }

    private fun countTotal() {
        tv_total_order.text = totalCount.toString()
        tv_total_price.text = totalPrice.toLong().let { kursIndonesia?.format(it) }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}