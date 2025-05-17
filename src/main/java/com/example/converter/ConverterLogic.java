package com.example.converter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Логика для управления параметрами конвертации видео.
 */
public class ConverterLogic {
    private final String[] formats = {"mp4", "avi", "mkv", "wmv"};
    private final Map<String, String[]> videoCodecs = new LinkedHashMap<>() {{
        put("mp4", new String[]{"libx264", "libx265", "mpeg4"});
        put("avi", new String[]{"mpeg4", "h264", "mjpeg"});
        put("mkv", new String[]{"libx264", "libx265", "vp9"});
        put("wmv", new String[]{"wmv2", "msmpeg4", "wmv1"});
    }};
    private final Map<String, String[]> audioCodecs = new LinkedHashMap<>() {{
        put("mp4", new String[]{"aac", "libmp3lame", "flac"});
        put("avi", new String[]{"mp3", "aac", "pcm_s16le"});
        put("mkv", new String[]{"aac", "opus", "vorbis"});
        put("wmv", new String[]{"wmav2", "mp3", "aac"});
    }};
    private final String[] bitrates = {"500k", "1M", "2M", "5M", "10M", "20M"};

    /**
     * Получить список доступных форматов.
     */
    public String[] getFormats() {
        return formats;
    }

    /**
     * Получить список видеокодеков для указанного формата.
     */
    public String[] getVideoCodecs(String format) {
        return videoCodecs.getOrDefault(format, new String[]{});
    }

    /**
     * Получить список аудиокодеков для указанного формата.
     */
    public String[] getAudioCodecs(String format) {
        return audioCodecs.getOrDefault(format, new String[]{});
    }

    /**
     * Получить список битрейтов.
     */
    public String[] getBitrates() {
        return bitrates;
    }

    /**
     * Сформировать имя выходного файла на основе формата и времени.
     */
    public String generateOutputFileName(String format, Date timestamp) {
        String timeString = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(timestamp);
        return "converted_" + timeString + "." + format;
    }
}
