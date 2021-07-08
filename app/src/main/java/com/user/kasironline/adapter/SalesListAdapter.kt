package com.user.kasironline.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.user.kasironline.R
import com.user.kasironline.activity.DetailOrderActivity
import com.user.kasironline.model.SalesModel
import com.user.kasironline.utils.Constant.TOTAL_COUNT
import com.user.kasironline.utils.Constant.TOTAL_PRICE
import com.user.kasironline.utils.Constant.USER_ID
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.sales_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class SalesListAdapter(
    options: FirestoreRecyclerOptions<SalesModel>
) :
    FirestoreRecyclerAdapter<SalesModel, SalesListAdapter.ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sales_item, parent, false)

        return ViewHolder(view)
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, item: SalesModel) {
        val formatter = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        val getKey = snapshots.getSnapshot(position).id
        val getContext = holder.itemView.context
        val getUsername = item.name.toString()
        val getPhone = item.phone.toString()
        val getDate = formatter.format(item.datetime)
        val getTotalCount = item.totalCount.toString()
        val getTotalPrice = item.totalPrice.toString()
        val getInformation = item.information.toString()

        holder.apply {
            containerView.tv_username.text = "Nama: $getUsername"
            containerView.tv_phone.text = "No. HP: $getPhone"
            containerView.tv_datetime.text = "Tanggal: $getDate"
            containerView.tv_information.text = "Keterangan: $getInformation"
            containerView.setOnClickListener {
                val intent = Intent(getContext, DetailOrderActivity::class.java)
                intent.putExtra(USER_ID, getKey)
                intent.putExtra(TOTAL_COUNT, getTotalCount)
                intent.putExtra(TOTAL_PRICE, getTotalPrice)
                getContext.startActivity(intent)
            }
        }
    }

    inner class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer
}