package com.budiyev.android.imageloader;

import java.io.File;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class FileDescriptorFactory implements DescriptorFactory<File> {
    @NonNull
    @Override
    public DataDescriptor<File> newDescriptor(@NonNull File data, @Nullable Size size) {
        return new FileDataDescriptor(data, size);
    }
}
