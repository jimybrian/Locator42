package com.dzworks.locator42

fun Double.formatString(digits: Int) : String {
    return "%.${digits}f".format(this)
}