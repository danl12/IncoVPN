package com.danl.incovpn.util

import java.util.*

fun String.toImageUrl() = "$IMAGE_URL$this.svg"
fun String.toDisplayCountry(): String = Locale("", this).displayCountry
