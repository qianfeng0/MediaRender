package com.android.mediarender;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class SurfaceVideoActivity extends FragmentActivity implements View.OnClickListener{
    private final String TAG = "SurfaceVideoActivity";

    private final String TestUrl = "http://devimages.apple.com/iphone/samples/bipbop/gear4/prog_index.m3u8";
    private MediaPlayer mPlayer;
    private SurfaceView mSurfaceVeiw;
    private SurfaceHolder mSurfaceHolder;

    private Button mStartBtn;
    private Button mPauseBtn;
    private Button mResumeBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_activity);

        initView();
    }

    void initView() {
        mSurfaceVeiw = findViewById(R.id.surface);
        mSurfaceVeiw.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated: ");
                mSurfaceHolder = surfaceHolder;
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged: start" + ",i:" + i + ",i1:" + i1 + ",i2:" + i2);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed: ");
                if (mPlayer != null) {
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                }
                mSurfaceHolder = null;
            }
        });


        mStartBtn = findViewById(R.id.button_start);
        mStartBtn.setOnClickListener(this);

        mPauseBtn = findViewById(R.id.button_pause);
        mPauseBtn.setOnClickListener(this);

        mResumeBtn = findViewById(R.id.button_resume);
        mResumeBtn.setOnClickListener(this);
    }

    void startPlayer(String url) {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(url);
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d(TAG, "onPrepared: ");
                    mediaPlayer.start();
                }
            });
            mPlayer.setDisplay(mSurfaceHolder);
            mPlayer.prepareAsync();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_start: {
                startPlayer(TestUrl);
                break;
            }
            case R.id.button_pause: {
                if (mPlayer != null) {
                    mPlayer.pause();
                }
                break;
            }
            case R.id.button_resume: {
                if (mPlayer != null) {
                    mPlayer.start();
                }
                break;
            }
            default: {
                break;
            }
        }
    }
}
