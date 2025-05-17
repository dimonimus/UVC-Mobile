package com.example.converter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST_CODE = 2;
    private VideoView videoView;
    private Button playPauseButton;
    private Button stopButton;
    private Button openButton;
    private SeekBar seekBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;
    private Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.videoView);
        playPauseButton = findViewById(R.id.playPauseButton);
        stopButton = findViewById(R.id.stopButton);
        openButton = findViewById(R.id.openButton);
        seekBar = findViewById(R.id.seekBar);

        // Получаем URI из Intent
        videoUri = getIntent().getParcelableExtra("videoUri");
        if (videoUri != null) {
            setupVideo(videoUri);
        }

        // Обработчики кнопок
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        stopButton.setOnClickListener(v -> stopVideo());
        openButton.setOnClickListener(v -> openVideoFile());

        // Настройка SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateProgressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isPlaying) {
                    handler.post(updateProgressRunnable);
                }
            }
        });

        // Обработка завершения видео
        videoView.setOnCompletionListener(mp -> {
            isPlaying = false;
            playPauseButton.setText("Пуск");
            seekBar.setProgress(0);
            handler.removeCallbacks(updateProgressRunnable);
        });

        // Обработка ошибок
        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Ошибка воспроизведения видео", Toast.LENGTH_SHORT).show();
            stopVideo();
            return true;
        });
    }

    private void setupVideo(Uri uri) {
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(mp -> {
            seekBar.setMax(videoView.getDuration());
            playVideo(); // Автоматически начать воспроизведение
        });
    }

    private void togglePlayPause() {
        if (isPlaying) {
            videoView.pause();
            playPauseButton.setText("Пуск");
            isPlaying = false;
            handler.removeCallbacks(updateProgressRunnable);
        } else {
            playVideo();
        }
    }

    private void playVideo() {
        videoView.start();
        playPauseButton.setText("Пауза");
        isPlaying = true;
        handler.post(updateProgressRunnable);
    }

    private void stopVideo() {
        videoView.stopPlayback();
        playPauseButton.setText("Пуск");
        isPlaying = false;
        seekBar.setProgress(0);
        handler.removeCallbacks(updateProgressRunnable);
    }

    private void openVideoFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            videoUri = data.getData();
            if (videoUri != null) {
                stopVideo();
                setupVideo(videoUri);
            }
        }
    }

    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                int currentPosition = videoView.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressRunnable);
    }
}