package com.example.converter;

import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.Date;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Модульные тесты для класса ConverterLogic.
 */
public class ConverterLogicTest {
    private ConverterLogic converterLogic;

    @Before
    public void setUp() {
        converterLogic = new ConverterLogic();
    }

    @Test
    public void testGetFormats() {
        String[] expected = {"mp4", "avi", "mkv", "wmv"};
        assertArrayEquals("Список форматов должен соответствовать ожидаемому", expected, converterLogic.getFormats());
    }

    @Test
    public void testGetVideoCodecsForMp4() {
        String[] expected = {"libx264", "libx265", "mpeg4"};
        assertArrayEquals("Видеокодеки для mp4 должны соответствовать ожидаемым", expected, converterLogic.getVideoCodecs("mp4"));
    }

    @Test
    public void testGetVideoCodecsForInvalidFormat() {
        String[] expected = {};
        assertArrayEquals("Для неизвестного формата возвращается пустой массив видеокодеков", expected, converterLogic.getVideoCodecs("invalid"));
    }

    @Test
    public void testGetAudioCodecsForMkv() {
        String[] expected = {"aac", "opus", "vorbis"};
        assertArrayEquals("Аудиокодеки для mkv должны соответствовать ожидаемым", expected, converterLogic.getAudioCodecs("mkv"));
    }

    @Test
    public void testGetAudioCodecsForInvalidFormat() {
        String[] expected = {};
        assertArrayEquals("Для неизвестного формата возвращается пустой массив аудиокодеков", expected, converterLogic.getAudioCodecs("invalid"));
    }

    @Test
    public void testGetBitrates() {
        String[] expected = {"500k", "1M", "2M", "5M", "10M", "20M"};
        assertArrayEquals("Список битрейтов должен соответствовать ожидаемому", expected, converterLogic.getBitrates());
    }

    @Test
    public void testGenerateOutputFileName() {
        Date timestamp = new Date(1623916800000L); // 2021-06-17 12:00:00
        String format = "mp4";
        String expected = "converted_20210617_120000.mp4";
        String result = converterLogic.generateOutputFileName(format, timestamp);
        assertEquals("Имя выходного файла должно быть сформировано корректно", expected, result);
    }
}