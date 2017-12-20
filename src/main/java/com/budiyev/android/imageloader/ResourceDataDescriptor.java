package com.budiyev.android.imageloader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ResourceDataDescriptor extends BaseDataDescriptor<Integer> {

    public ResourceDataDescriptor(@NonNull Integer data, @NonNull String keyBase, @Nullable Size size) {
        super(data, keyBase, size);
    }
}
