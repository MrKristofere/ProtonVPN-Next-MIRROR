package ru.protonmod.next.vpn

data class ObfuscationParams(
    val jc: Int,
    val jmin: Int,
    val jmax: Int,
    val s1: Int,
    val s2: Int,
    val s3: Int = 0,
    val s4: Int = 0,
    val h1: String,
    val h2: String,
    val h3: String,
    val h4: String,
    val i1: String,
    val i2: String = "",
    val i3: String = "",
    val i4: String = "",
    val i5: String = ""
)
