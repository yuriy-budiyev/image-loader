package com.budiyev.android.imageloader

interface DataDescriptor<T> {

    val data: T

    val key: String

    val location: DataLocation
}
