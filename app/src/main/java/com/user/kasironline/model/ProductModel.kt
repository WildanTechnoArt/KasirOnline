package com.user.kasironline.model

data class ProductModel(
    var image: String? = null,
    var name: String? = null,
    var price: String? = null,
    var priceValue: Int? = null,
    var discount: Int? = null,
    var stock: Int? = null
)