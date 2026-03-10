package com.web.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MediaUtil {

    /**
     * 使用 ffprobe 获取媒体文件时长（毫秒）
     * @param filePath 文件的绝对路径
     * @return 时长（毫秒），如果解析失败则返回 0
     * @throws IOException 调用 ffprobe 失败时抛出
     */
    public static long getDurationMs(String filePath) throws IOException {
        // 1. 构建 ProcessBuilder 来运行 ffprobe 命令
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",                      // 命令名
                "-v", "error",                   // 只输出错误信息，减少无用输出
                "-show_entries", "format=duration", // 只显示 format 中的 duration 字段
                "-of", "default=noprint_wrappers=1:nokey=1", // 输出格式：只输出值，不输出键名
                filePath                         // 要分析的文件路径
        );

        // 2. 启动进程
        Process process = pb.start();

        // 3. 读取进程的标准输出流
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();     // 读取第一行（也是唯一一行）
            if (line != null && !line.trim().isEmpty()) {
                double seconds = Double.parseDouble(line.trim()); // 将字符串转为 double
                return (long) (seconds * 1000);   // 秒转毫秒，返回 long
            }
        }

        // 4. 如果没有读到时长，返回 0
        return 0;
    }
}