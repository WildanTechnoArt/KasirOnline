package com.user.kasironline.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.user.kasironline.fragment.OtherCostsFragment
import com.user.kasironline.fragment.ProductListFragment
import com.user.kasironline.fragment.PurchaseListFragment
import com.user.kasironline.fragment.SalesFragment

class PagerAdapter(fm: AppCompatActivity) :
    FragmentStateAdapter(fm) {

    private val pages =
        listOf(
            ProductListFragment(),
            SalesFragment(),
            PurchaseListFragment(),
            OtherCostsFragment()
        )

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun createFragment(position: Int): Fragment {
        return pages[position]
    }
}