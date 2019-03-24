package com.vd.diff.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class FastAccessFile extends RandomAccessFile {

    private final File file;

    public FastAccessFile(File file, String mode) throws FileNotFoundException {
        super(file, mode);
        this.file = file;
    }

    public String getPath() {
        return file.getPath();
    }
}
