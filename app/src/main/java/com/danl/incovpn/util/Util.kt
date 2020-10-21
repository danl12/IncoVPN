package com.danl.incovpn.util

import com.danl.incovpn.IncoVPNApp.Companion.IMAGE_URL
import java.util.*

fun String.toImageUrl() = "$IMAGE_URL$this.svg"
fun String.toDisplayCountry(): String = Locale("", this).displayCountry
