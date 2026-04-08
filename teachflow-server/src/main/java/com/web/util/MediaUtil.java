package com.web.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MediaUtil {


    public static long getDurationMs(String filePath) throws IOException {

        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
        );


        Process process = pb.start();


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                double seconds = Double.parseDouble(line.trim());
                return (long) (seconds * 1000);
            }
        }


        return 0;
    }
}