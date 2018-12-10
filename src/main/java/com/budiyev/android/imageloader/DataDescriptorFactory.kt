package com.budiyev.android.imageloader

interface DataDescriptorFactory<T> {

    fun createDescriptor(data: T): DataDescriptor<T>
}
