package com.example.hwhong.mediastreaming;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button button;
    private EditText editText;
    private ProgressBar bar;

    private MediaPlayer player;
    private SurfaceView view;
    private SurfaceHolder holder;

    //check if the mediaplayer is prepared
    private boolean prepared = false;
    //check if the video is completed playing
    private boolean completed = false;

    //length of video
    private int fileLength;
    private final Handler handler = new Handler();
    private WifiManager.WifiLock lock;

    private final String media = "http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        editText = (EditText) findViewById(R.id.editText);
        editText.setText(media);

        bar = (ProgressBar) findViewById(R.id.progressBar);
        bar.setMax(99);
        bar.setOnTouchListener(new BarListener());

        WifiManager wifiManager = ((WifiManager) getSystemService(Context.WIFI_SERVICE));
        lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "WifiLock");

        player = new MediaPlayer();
        player.setOnPreparedListener(new PreparedListener());
        player.setOnCompletionListener(new CompletionListener());
        player.setOnBufferingUpdateListener(new BufferingUpdateLisener());
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //Provides a dedicated drawing surface embedded inside of a view hierarchy.
        view = (SurfaceView) findViewById(R.id.surfaceView);
        holder = view.getHolder();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.button:

                if(!prepared) {
                    new Task().execute();
                } else if (prepared) {

                    //gets the length of the video being played
                    fileLength = player.getDuration();

                    if(player.isPlaying()) {

                        player.pause();

                    } else {

                        player.start();

                    }

                    progressUpdate();
                }

                break;
        }

    }

    private class BarListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            switch (view.getId()) {
                case R.id.progressBar:
                    if(player.isPlaying()) {
                        ProgressBar progressBar = (ProgressBar) view;
                        int seconds = (fileLength / 100) * bar.getProgress();
                        player.seekTo(seconds);
                    }
                    break;
            }

            return false;
        }
    }

    private class PreparedListener implements MediaPlayer.OnPreparedListener {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {

            prepared = true;
            completed = false;

            fileLength = player.getDuration();

            player.start();
            progressUpdate();
            Toast.makeText(getApplicationContext(), "Video Started!", Toast.LENGTH_SHORT).show();
        }
    }

    private class CompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {

            completed = true;
            Toast.makeText(getApplicationContext(), "Video Ended!", Toast.LENGTH_SHORT).show();
        }
    }

    private class BufferingUpdateLisener implements MediaPlayer.OnBufferingUpdateListener {

        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
            bar.setSecondaryProgress(i);
        }
    }

    private class Task extends AsyncTask<Void, Integer, Boolean> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(MainActivity.this, "Preparing...", "Please Hold!");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {

                player.setDataSource(media);
                player.setDisplay(holder);
                player.prepare();
                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean) {
                dialog.dismiss();
            } else {
                Toast.makeText(getApplicationContext(), "Preparation Failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void progressUpdate() {
        if(player == null) {
            return;
        }

        float deci = (float) player.getCurrentPosition()/fileLength;
        int progress = (int)(deci * 100);

        bar.setProgress(progress);

        if(player.isPlaying()) {
            Runnable notification = new Runnable() {
                @Override
                public void run() {
                    progressUpdate();
                }
            };
            handler.postDelayed(notification, 1000);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(lock != null) {
            lock.acquire();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(player != null) {

            player.release();
            player = null;

        }

        if(lock != null) {

            lock.release();

        }
    }
}
