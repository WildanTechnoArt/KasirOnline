package com.user.kasironline.utils

import android.content.Context
import android.os.Environment
import com.user.kasironline.R
import java.io.File

object Common {
    fun getAppPath(context: Context): String {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                .toString() + File.separator
                    + context.resources.getString(R.string.app_name)
                    + File.separator
        )
        var isDirectoryCreated = dir.exists()
        if (!isDirectoryCreated) {
            isDirectoryCreated = dir.mkdirs()
        }

        var a: String? = null
        if (isDirectoryCreated) {
            a = dir.path + File.separator
        }
        return a.toString()
    }
}