package com.budiyev.android.imageloader;

import java.io.FileDescriptor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

final class FileDescriptorDescriptorFactory implements DescriptorFactory<FileDescriptor> {
    @NonNull
    @Override
    public DataDescriptor<FileDescriptor> newDescriptor(@NonNull FileDescriptor data, @Nullable Size size) {
        return new FileDescriptorDataDescriptor(data, size);
    }
}
