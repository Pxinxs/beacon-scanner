package com.pxinxs.beaconscanner.utils

import android.content.Context
import android.widget.Toast

/**
 *Created by Anton on 08.02.2018.
 */

fun Context.toast(message: String, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, message, length).show()
}
