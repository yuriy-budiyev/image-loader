package com.budiyev.android.imageloader

/**
 * Data location
 */
enum class DataLocation {

    /**
     * Data is located at remote storage (ex. website)
     */
    REMOTE,

    /**
     * Data is located at device's local storage,
     * local images will not be cached in storage cache except sized and transformed ones
     */
    LOCAL
}
