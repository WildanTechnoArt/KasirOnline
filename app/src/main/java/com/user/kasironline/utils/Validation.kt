package com.user.kasironline.utils

import android.text.TextUtils

class Validation {

    companion object {
        fun validateFields(input: String): Boolean {
            return TextUtils.isEmpty(input)
        }
    }
}