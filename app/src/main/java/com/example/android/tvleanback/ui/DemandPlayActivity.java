package com.example.android.tvleanback.ui;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.example.android.tvleanback.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

public class DemandPlayActivity extends Activity {


    private static final String KEY_PLAY_WHEN_READY = "play_when_ready";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";

    // video player
    private PlayerView playerView;
    private SimpleExoPlayer player;

    private Timeline.Window         window;
    private DataSource.Factory      mediaDataSourceFactory;
    private DefaultTrackSelector trackSelector;
    private TrackGroupArray lastSeenTrackGroupArray;
    private boolean                 shouldAutoPlay;
    private BandwidthMeter bandwidthMeter;

    private boolean     playWhenReady;
    private int         currentWindow;
    private long        playbackPosition;

    private boolean     isDirectURL;

    private String streamUrl;

    // Key Press
    private View.OnKeyListener m_listenerKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_demandplay);
        //setContentView(R.layout.single_grid);
        streamUrl = "";
        if (getIntent().hasExtra(VideoDetailsActivity.VIDEO))
            streamUrl = getIntent().getStringExtra(VideoDetailsActivity.VIDEO);

        //video player
        if (savedInstanceState == null) {
            playWhenReady = true;
            currentWindow = 0;
            playbackPosition = 0;
        } else {
            playWhenReady = savedInstanceState.getBoolean(KEY_PLAY_WHEN_READY);
            currentWindow = savedInstanceState.getInt(KEY_WINDOW);
            playbackPosition = savedInstanceState.getLong(KEY_POSITION);
        }

        // Load All Demands
        initView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        shouldAutoPlay = true;
        bandwidthMeter = new DefaultBandwidthMeter();
        mediaDataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "mediaPlayerSample"), (TransferListener<? super DataSource>) bandwidthMeter);
        window = new Timeline.Window();

        //streamUrl = "https://streaming1.cymtv.com:30443/imtv/cnn_h264/playlist.m3u8";
        //streamUrl = "http://team.stvhosting.com:25461/TaylorTodd/beh97MWXpv/14121";
        //streamUrl = "http://163.172.102.165:25461/movie/Dev03/Dev03/7077.mp4";
        //streamUrl = "http://163.172.102.165:25461/movie/Dev03/Dev03/7077.mp4";
        //streamUrl = "http://163.172.102.165:25461/movie/taylor/taylor/7082.mp4";
        //streamUrl = "http://awe-website-karouselnetwork-1154.s3-website.eu-central-1.amazonaws.com/Videos/movies/wonder_woman/index.m3u8";
        //streamUrl = "http://awe-website-karouselnetwork-1154.s3-website.eu-central-1.amazonaws.com/Videos/movies/beauty_and_the_beast/index.m3u8";

        //streamUrl = " http://primemediatv.co.uk:80/movie/TayLorTodd100Restreram/TayLorTodd100Restreram/16381.mkv";
        //Utils.disableSSLCertificateChecking();
        SetKeyListener();

    }
    public void initView(){

    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            //initializePlayer();
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        updateStartPosition();

        outState.putBoolean(KEY_PLAY_WHEN_READY, playWhenReady);
        outState.putInt(KEY_WINDOW, currentWindow);
        outState.putLong(KEY_POSITION, playbackPosition);
        super.onSaveInstanceState(outState);
    }
    private void updateStartPosition() {
        if (player == null) return;

        playbackPosition = player.getCurrentPosition();
        currentWindow = player.getCurrentWindowIndex();
        playWhenReady = player.getPlayWhenReady();
    }
    private void initializePlayer() {
        if(player != null)
            return;

        playerView = findViewById(R.id.ID_HEAD_VIDEO_VIEW);

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);

        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        lastSeenTrackGroupArray = null;

        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        playerView.setPlayer(player);

        playerView.setControllerShowTimeoutMs(1000);

        player.setPlayWhenReady(shouldAutoPlay);

        MediaSource mediaSource;
        if(streamUrl.endsWith(".m3u8"))
            mediaSource = new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(Uri.parse(streamUrl));
        else
            mediaSource = new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(Uri.parse(streamUrl));

        boolean haveStartPosition = currentWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(currentWindow, playbackPosition);
        }

        player.prepare(mediaSource, !haveStartPosition, false);
    }
    private void releasePlayer() {
        if (player != null) {
            updateStartPosition();
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            player = null;
            trackSelector = null;
        }
    }
    public void SetKeyListener(){
        m_listenerKey = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                switch(keyEvent.getKeyCode()){
                    case KeyEvent.KEYCODE_BACK:
                        if (keyEvent.getAction() != KeyEvent.ACTION_UP)
                            return true;
                        Log.i("finish","finish");
                        finish();
                        break;
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                        if (keyEvent.getAction() != KeyEvent.ACTION_UP)
                            return true;
                        Log.i("Enter=","ok");
                        playerView.showController();
                        break;
//                    case KeyEvent.KEYCODE_DPAD_DOWN:
//                        if (keyEvent.getAction() != KeyEvent.ACTION_UP)
//                            return true;
//
//                        break;
//                    case KeyEvent.KEYCODE_DPAD_UP:
//                        if (keyEvent.getAction() != KeyEvent.ACTION_UP)
//                            return true;
//
//                        break;
//                    case KeyEvent.KEYCODE_DPAD_LEFT:
//                        if (keyEvent.getAction() != KeyEvent.ACTION_UP)
//                            return true;
//
//                        break;
//                    case KeyEvent.KEYCODE_DPAD_RIGHT:
//                        if (keyEvent.getAction() != KeyEvent.ACTION_UP)
//                            return true;
//                    case KeyEvent.KEYCODE_MENU:
//                        if(keyEvent.getAction() != KeyEvent.ACTION_DOWN)
//                            return true;
//                        Log.i("Menu=","menu");
                    //break;
                    default:
                        break;
                }
                return false;
            }
        };

        // Set Focus to Content View, if else, Key Event won't be accepted at once
        findViewById(R.id.activity_demandplay).getRootView().clearFocus();
        findViewById(R.id.activity_demandplay).setFocusable(true);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //findViewById(R.id.grid_demandthumb).setFocusable(true);
        findViewById(R.id.activity_demandplay).setOnKeyListener(m_listenerKey);
        //playerView.setFocusable(true);
        //playerView.setOnKeyListener(m_listenerKey);
    }
}