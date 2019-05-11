package ru.geekbrains.belikov.cloud.common;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileController {

    public static void delete(String item) throws IOException {
        if (Files.isDirectory(Paths.get(item))){
            File dir = new File(item);
            File[] files = dir.listFiles();
            if (files.length == 0) {
                dir.delete();
                return;
            }
            System.out.println();
            for (File f: files
            ) {
                System.out.println(item + f.getName());
                delete(item + f.getName()+ "/");
            }
        } else {
            Files.delete(Paths.get(item));
            return;
        }
        delete(item);
    }
}
