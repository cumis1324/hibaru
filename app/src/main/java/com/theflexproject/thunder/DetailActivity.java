package com.theflexproject.thunder;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;

import com.theflexproject.thunder.fragments.PlayerFragment;
import com.theflexproject.thunder.model.Movie;
import com.theflexproject.thunder.model.SharedViewModel;

@UnstableApi
public class DetailActivity extends AppCompatActivity {
    private SharedViewModel sharedViewModel;
    private PlayerFragment movieDetailsFragment;
    private TextView textView;
    private Intent intent;
    private Rational aspectRatio;
    PictureInPictureParams params;
    private OnUserLeaveHintListener userLeaveHintListener;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_detail);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        textView = findViewById(R.id.tesText);
        bukaIntent();
       }

    private void bukaIntent() {
        intent = getIntent();
        int itemId = intent.getIntExtra("itemId", -1);
        String itemType = intent.getStringExtra("type");
        textView.setText(itemType);
        aspectRatio = new Rational(16, 9);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
        }
        if (itemType.equals("movie")) {
            movieDetailsFragment = new PlayerFragment(itemId, true);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in,R.anim.fade_out,R.anim.fade_in,R.anim.fade_out)
                    .replace(R.id.detailFrame,movieDetailsFragment).commit();
        }
    }
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (userLeaveHintListener != null) {
            userLeaveHintListener.onUserLeaveHint();
        }
    }

    public void setOnUserLeaveHintListener(OnUserLeaveHintListener listener) {
        this.userLeaveHintListener = listener;
    }

    public interface OnUserLeaveHintListener {
        void onUserLeaveHint();
    }

}
