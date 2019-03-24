package com.vd.diff.utils;

import com.google.common.base.Preconditions;
import com.vd.diff.file.FastAccessFile;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class FileUtils {

    public static void deleteDirectory(File dir) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(dir);
    }

    public static File getFileFromResources(String fileName, Class clazz) {
        URL resourceUrl = clazz.getResource(fileName);
        try {
            return Paths.get(resourceUrl.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeSilently(FastAccessFile file) {
        String pathToFile = file.getPath();
        closeFile(file, pathToFile);
    }

    private static void closeFile(RandomAccessFile file, String pathToFile) {
        try {
            file.close();
        } catch (IOException ignored) {
            System.out.println("Error on closing file: " + pathToFile);
        }
    }

    public static File createNew(String name) {
        File destination = new File(name);
        if (destination.exists()) {
            Preconditions.checkState(destination.delete());
        }
        return destination;
    }
}
