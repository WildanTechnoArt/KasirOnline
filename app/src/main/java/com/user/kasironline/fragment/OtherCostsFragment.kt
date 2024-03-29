package com.user.kasironline.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.user.kasironline.R
import com.user.kasironline.adapter.CostsListAdapter
import com.user.kasironline.database.SharedPrefManager
import com.user.kasironline.model.CostsModel
import com.user.kasironline.view.CostListView
import kotlinx.android.synthetic.main.fragment_tab_item.*

class OtherCostsFragment : Fragment(), CostListView {

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
        tv_not_data.text = getString(R.string.no_costs)
        username = SharedPrefManager.getInstance(context)?.getUsername.toString()

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
            .collection("costs")
            .orderBy("date")

        val options = FirestoreRecyclerOptions.Builder<CostsModel>()
            .setQuery(query, CostsModel::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = CostsListAdapter(options, this)
        rv_item?.adapter = adapter
    }

    private fun checkData() {
        swipe_refresh?.isRefreshing = true

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(username.toString())
            .collection("costs")
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

    override fun onDelete(id: String) {
        swipe_refresh?.isRefreshing = true

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(username.toString())
            .collection("costs")
            .document(id)
            .delete()
            .addOnSuccessListener {
                db.collection("payment")
                    .document(id)
                    .delete()
                    .addOnSuccessListener {
                        swipe_refresh?.isRefreshing = false
                    }.addOnFailureListener {
                        swipe_refresh?.isRefreshing = false
                        Toast.makeText(context, it.localizedMessage?.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }
            }.addOnFailureListener {
                swipe_refresh?.isRefreshing = false
                Toast.makeText(context, it.localizedMessage?.toString(), Toast.LENGTH_SHORT).show()
            }
    }
}