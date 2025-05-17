package com.example.converter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Главная активность для управления конвертацией видеофайлов.
 */
public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TEMP_DIR_NAME = "temp";
    private static final String OUTPUT_DIR_NAME = "ConvertedVideos";

    private Uri inputUri;
    private File tempInputFile;
    private TextView statusText;
    private Button openButton;
    private Button convertButton;
    private Button playButton;
    private Spinner formatSpinner;
    private Spinner videoCodecSpinner;
    private Spinner audioCodecSpinner;
    private Spinner bitrateSpinner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long executionId;

    // Доступные форматы и кодеки
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        setupSpinners();
        setupButtonListeners();
        Config.enableLogCallback(this::updateStatusLog);
    }

    /**
     * Инициализация пользовательского интерфейса.
     */
    private void initializeUI() {
        openButton = findViewById(R.id.openButton);
        convertButton = findViewById(R.id.convertButton);
        playButton = findViewById(R.id.playButton);
        statusText = findViewById(R.id.statusText);
        formatSpinner = findViewById(R.id.formatSpinner);
        videoCodecSpinner = findViewById(R.id.videoCodecSpinner);
        audioCodecSpinner = findViewById(R.id.audioCodecSpinner);
        bitrateSpinner = findViewById(R.id.bitrateSpinner);

        statusText.setTextColor(Color.BLACK);
        openButton.setTextColor(Color.WHITE);
        convertButton.setTextColor(Color.WHITE);
        playButton.setTextColor(Color.WHITE);
        convertButton.setEnabled(false);
    }

    /**
     * Настройка спиннеров с динамическим обновлением кодеков.
     */
    private void setupSpinners() {
        ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, formats);
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        formatSpinner.setAdapter(formatAdapter);
        formatSpinner.setSelection(0);

        ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, bitrates);
        bitrateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bitrateSpinner.setAdapter(bitrateAdapter);
        bitrateSpinner.setSelection(2);

        formatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCodecSpinners();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        updateCodecSpinners();
    }

    /**
     * Обновление списков кодеков в зависимости от выбранного формата.
     */
    private void updateCodecSpinners() {
        String selectedFormat = formats[formatSpinner.getSelectedItemPosition()];
        updateSpinner(videoCodecSpinner, videoCodecs.get(selectedFormat));
        updateSpinner(audioCodecSpinner, audioCodecs.get(selectedFormat));
    }

    /**
     * Обновление содержимого спиннера.
     */
    private void updateSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * Настройка обработчиков событий для кнопок.
     */
    private void setupButtonListeners() {
        openButton.setOnClickListener(v -> handlePermission(() -> openFile()));
        convertButton.setOnClickListener(v -> handlePermission(this::convertFile));
        playButton.setOnClickListener(v -> {
            if (inputUri != null) openVideoPlayer(inputUri);
            else {
                Toast.makeText(this, "Выберите файл сначала", Toast.LENGTH_SHORT).show();
                openFile();
            }
        });
    }

    /**
     * Запуск активности видеоплеера с передачей URI видео.
     */
    private void openVideoPlayer(Uri videoUri) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoUri", videoUri.toString()); // Передаём URI как строку
        startActivity(intent);
    }

    /**
     * Обработка действий с проверкой разрешений.
     */
    private void handlePermission(Runnable action) {
        if (!checkPermission()) requestPermission(action);
        else action.run();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(Runnable onGranted) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFile();
        } else {
            Toast.makeText(this, "Разрешение на хранилище необходимо", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && requestCode == PICK_FILE_REQUEST_CODE) {
            inputUri = data.getData();
            try {
                tempInputFile = copyFileToTemp(inputUri);
                statusText.setText("Выбран файл: " + getFileName(inputUri));
                convertButton.setEnabled(true);
            } catch (Exception e) {
                showError("Ошибка копирования: " + e.getMessage());
            }
        }
    }

    private File copyFileToTemp(Uri uri) throws Exception {
        File tempDir = createTempDir();
        String fileName = getFileName(uri);
        File tempFile = new File(tempDir, "input_" + System.currentTimeMillis() + "_" + fileName);

        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private File createTempDir() {
        File tempDir = new File(getCacheDir(), TEMP_DIR_NAME);
        if (!tempDir.exists()) tempDir.mkdirs();
        return tempDir;
    }

    private void convertFile() {
        if (inputUri == null || tempInputFile == null) {
            Toast.makeText(this, "Выберите файл сначала", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDir = createOutputDir();
        if (outputDir == null) return;

        String selectedFormat = formats[formatSpinner.getSelectedItemPosition()];
        String videoCodec = (String) videoCodecSpinner.getSelectedItem();
        String audioCodec = (String) audioCodecSpinner.getSelectedItem();
        String bitrate = (String) bitrateSpinner.getSelectedItem();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String outputPath = new File(outputDir, "converted_" + timestamp + "." + selectedFormat).getAbsolutePath();

        setupConversionUI();

        String[] command = {
                "-i", tempInputFile.getAbsolutePath(),
                "-c:v", videoCodec,
                "-c:a", audioCodec,
                "-b:v", bitrate,
                "-r", "30",
                outputPath
        };

        executionId = FFmpeg.executeAsync(command, this::handleConversionResult);
    }

    private File createOutputDir() {
        File outputDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), OUTPUT_DIR_NAME);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            outputDir = new File(getCacheDir(), OUTPUT_DIR_NAME);
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                showError("Не удалось создать директорию");
                return null;
            }
        }
        return outputDir;
    }

    private void setupConversionUI() {
        openButton.setEnabled(false);
        convertButton.setText("Отмена");
        convertButton.setOnClickListener(v -> FFmpeg.cancel(executionId));
        statusText.setText("Конвертация...");
    }

    private void handleConversionResult(long id, int returnCode) {
        handler.post(() -> {
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                statusText.setText("Конвертация завершена!\nСохранено в: " + getOutputPath());
            } else {
                showError("Ошибка конвертации. Код: " + returnCode);
            }
            cleanup();
            resetUI();
        });
    }

    private String getOutputPath() {
        return new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), OUTPUT_DIR_NAME)
                .listFiles()[(int) (Math.random() * new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), OUTPUT_DIR_NAME).listFiles().length)].getAbsolutePath();
    }

    private void cleanup() {
        if (tempInputFile != null && tempInputFile.exists()) tempInputFile.delete();
    }

    private void resetUI() {
        openButton.setEnabled(true);
        convertButton.setText("Конвертировать");
        convertButton.setEnabled(false);
        convertButton.setOnClickListener(v -> convertFile());
        inputUri = null;
        tempInputFile = null;
    }

    private void showError(String message) {
        statusText.setText("Ошибка: " + message);
    }

    private String getFileName(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName == null) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        return fileName != null ? fileName : "unnamed_video";
    }

    private void updateStatusLog(com.arthenica.mobileffmpeg.LogMessage message) {
        handler.post(() -> statusText.append("\n" + message.getText()));
    }
}