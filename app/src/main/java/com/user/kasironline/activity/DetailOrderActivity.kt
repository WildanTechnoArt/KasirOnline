package com.user.kasironline.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.R
import com.user.kasironline.adapter.OrderListAdapter
import com.user.kasironline.model.CartModel
import com.user.kasironline.utils.Constant.TOTAL_COUNT
import com.user.kasironline.utils.Constant.TOTAL_PRICE
import com.user.kasironline.utils.Constant.USER_ID
import kotlinx.android.synthetic.main.activity_detail_order.*

class DetailOrderActivity : AppCompatActivity() {

    private var phone: String? = null
    private var mTotalCount: String? = null
    private var mTotalPrice: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_order)
        prepare()
        requestData()
    }

    private fun prepare() {
        phone = intent.getStringExtra(USER_ID).toString()
        mTotalCount = intent.getStringExtra(TOTAL_COUNT).toString()
        mTotalPrice = intent.getStringExtra(TOTAL_PRICE).toString()

        tv_total_order.text = mTotalCount
        tv_total_price.text = mTotalPrice

        rv_product?.layoutManager = LinearLayoutManager(this)
        rv_product?.setHasFixedSize(true)

        swipe_refresh?.setOnRefreshListener {
            requestData()
        }
    }

    private fun requestData() {
        val query = FirebaseFirestore.getInstance()
            .collection("sales")
            .document(phone.toString())
            .collection("order")

        val options = FirestoreRecyclerOptions.Builder<CartModel>()
            .setQuery(query, CartModel::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = OrderListAdapter(options)
        rv_product?.adapter = adapter
    }
}