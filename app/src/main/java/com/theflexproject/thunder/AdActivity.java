package com.theflexproject.thunder;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

import com.theflexproject.thunder.player.PlayerActivity;

public class AdActivity extends AppCompatActivity {

    private VideoView adVideoView;
    private TextView countdownText;
    private Button skipButton;
    private Intent intent;
    int uiOptions;
    View decorView;
    private int countdownTime = 7; // Waktu countdown dalam detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads);
        decorView = getWindow().getDecorView();

        String urlString = intent.getStringExtra("url");

        uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(uiOptions);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        adVideoView = findViewById(R.id.adVideoView);
        countdownText = findViewById(R.id.countdownText);
        skipButton = findViewById(R.id.skipButton);

        // URL iklan VAST yang Anda berikan
        String adUrl = "https://pubads.g.doubleclick.net/gampad/ads?sz=480x320|1024x768|1x1|640x480&iu=/23200225483/res&env=vp&impl=s&gdfp_req=1&output=vast&unviewed_position_start=1&url=[referrer_url]&description_url=[description_url]&correlator=[timestamp]";
        Uri adUri = Uri.parse(adUrl);

        adVideoView.setVideoURI(adUri);
        adVideoView.start();

        // Mulai countdown 7 detik
        new CountDownTimer(7000, 1000) {
            public void onTick(long millisUntilFinished) {
                // Update TextView setiap detik
                countdownText.setText(String.valueOf(countdownTime));
                countdownTime--;
            }

            public void onFinish() {
                // Countdown selesai, tampilkan tombol skip
                skipButton.setVisibility(View.VISIBLE);
                countdownText.setVisibility(View.GONE);
            }
        }.start();

        // Aksi tombol Skip Ad
        skipButton.setOnClickListener(v -> {
            // Akhiri AdActivity dan kembali ke MainActivity


            Intent in = new Intent(this, PlayerActivity.class);
            in.putExtra("url", urlString);
            startActivity(in);
            finish();
        });

        // Listener ketika iklan selesai
        adVideoView.setOnCompletionListener(mp -> {
            // Iklan selesai, kembali ke MainActivity
            Intent in = new Intent(this, PlayerActivity.class);
            in.putExtra("url", urlString);
            startActivity(in);
            finish();
        });

        // Listener untuk menangani error saat memutar video
        adVideoView.setOnErrorListener((mp, what, extra) -> {
            // Tangani error
            Intent in = new Intent(this, PlayerActivity.class);
            in.putExtra("url", urlString);
            startActivity(in);
            return true;
        });
    }
}

