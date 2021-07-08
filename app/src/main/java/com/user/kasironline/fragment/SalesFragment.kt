package com.user.kasironline.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.R
import com.user.kasironline.adapter.SalesListAdapter
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.model.SalesModel
import kotlinx.android.synthetic.main.fragment_tab_item.*

class SalesFragment : Fragment() {

    private var username: String? = null

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
        username = SharedPrefManager.getInstance(context)?.getUsername.toString()
        tv_not_data.text = getString(R.string.no_sales)

        rv_item?.layoutManager = LinearLayoutManager(context)
        rv_item?.setHasFixedSize(true)

        swipe_refresh?.setOnRefreshListener {
            checkData()
        }
    }

    private fun requestData() {
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(username.toString())
            .collection("sales")
            .orderBy("datetime")

        val options = FirestoreRecyclerOptions.Builder<SalesModel>()
            .setQuery(query, SalesModel::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = SalesListAdapter(options)
        rv_item?.adapter = adapter
    }

    private fun checkData() {
        swipe_refresh?.isRefreshing = true

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(username.toString())
            .collection("sales")
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
}