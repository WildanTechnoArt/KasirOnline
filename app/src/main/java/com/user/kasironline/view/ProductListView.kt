package com.user.kasironline.view

import com.user.kasironline.model.ProductModel

interface ProductListView {
    fun onEditProductDialog(id: String, data: ProductModel)
}