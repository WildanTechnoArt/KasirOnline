package com.user.kasironline.model

import java.util.*

data class PurchaseModel(
    var supplier: String? = null,
    var phone: String? = null,
    var name: String? = null,
    var price: String? = null,
    var date: Date? = null,
    var priceValue: Int? = null,
    var information: String? = null,
    var stock: Int? = null
)