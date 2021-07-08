package com.user.kasironline.adapter

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.*

class PdfDocumentAdapter(private val path: String) :
    PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
        } else {
            val builder = PrintDocumentInfo.Builder("Invoice Royal Truss")
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback.onLayoutFinished(builder.build(), newAttributes != oldAttributes)
        }
    }

    override fun onWrite(
        pages: Array<PageRange>,
        parcelFileDescriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        writeResultCallback: WriteResultCallback
    ) {
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            val file = File(path)
            `in` = FileInputStream(file)
            out = FileOutputStream(parcelFileDescriptor.fileDescriptor)
            val buff = ByteArray(16384)
            var size: Int
            while (`in`.read(buff).also { size = it } >= 0 && !cancellationSignal.isCanceled) {
                out.write(buff, 0, size)
            }
            if (cancellationSignal.isCanceled) {
                writeResultCallback.onWriteCancelled()
            } else {
                writeResultCallback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (ex: FileNotFoundException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            writeResultCallback.onWriteFailed(ex.message)
            ex.printStackTrace()
        } finally {
            try {
                `in`?.close()
                out?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }
}