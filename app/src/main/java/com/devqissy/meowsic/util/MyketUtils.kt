package com.devqissy.meowsic.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.devqissy.meowsic.R

fun openMyketForRating(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("myket://application?id=$packageName")))
        return
    } catch (_: ActivityNotFoundException) {}
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myket.ir/app/$packageName")))
        return
    } catch (_: ActivityNotFoundException) {}
    Toast.makeText(context, context.getString(R.string.rate_myket_not_installed), Toast.LENGTH_LONG).show()
}