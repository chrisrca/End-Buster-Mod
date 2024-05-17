package com.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class EndCityFinder {
    static {
        try {
            // Extract the DLL to a temporary directory
            String dllName = "find_biome_at.dll";
            File tempDir = Files.createTempDirectory("nativelib").toFile();
            File tempDll = new File(tempDir, dllName);

            try (InputStream is = EndCityFinder.class.getResourceAsStream("/native/" + dllName)) {
                if (is == null) {
                    throw new IOException("DLL not found in JAR: " + dllName);
                }

                Files.copy(is, tempDll.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("DLL copied to: " + tempDll.getAbsolutePath()); // Debug print
            }

            System.load(tempDll.getAbsolutePath());
            System.out.println("DLL loaded from: " + tempDll.getAbsolutePath()); // Debug print
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    // Declare the native method
    public native EndCity[] findEndCities(long seed, int centerX, int centerZ, int radius);

    // Test the native method
    public static void main(String[] args) {
        EndCityFinder finder = new EndCityFinder();
        EndCity[] cities = finder.findEndCities(-8169697951202909253L, 0, 0, 5000);
        for (EndCity city : cities) {
            System.out.println("End City at (" + city.getX() + ", " + city.getZ() + "), has ship: " + city.hasShip());
        }
    }
}
