package com.shohan.mediabridge.player;
import android.os.Bundle;import android.view.*;import android.widget.MediaController;
import androidx.appcompat.app.AppCompatActivity;
import com.shohan.mediabridge.databinding.ActivityVideoPlayerBinding;
import java.io.File;
public class VideoPlayerActivity extends AppCompatActivity {
    private ActivityVideoPlayerBinding b;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        b=ActivityVideoPlayerBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        if(getSupportActionBar()!=null)getSupportActionBar().hide();
        String path=getIntent().getStringExtra("path");
        String title=getIntent().getStringExtra("title");
        b.tvTitle.setText(title!=null?title:"Video");
        b.btnBack.setOnClickListener(v->finish());
        if(path==null||!new File(path).exists()){
            b.tvError.setVisibility(View.VISIBLE);
            b.tvError.setText("File not found: "+path);return;}
        MediaController mc=new MediaController(this);
        mc.setAnchorView(b.videoView);
        b.videoView.setMediaController(mc);
        b.videoView.setVideoPath(path);
        b.videoView.setOnPreparedListener(mp->{b.progressBar.setVisibility(View.GONE);b.videoView.start();});
        b.videoView.setOnErrorListener((mp,w2,e)->{
            b.tvError.setVisibility(View.VISIBLE);
            b.tvError.setText("Cannot play this video (code: "+w2+")");return true;});
        b.videoView.requestFocus();
    }
    @Override protected void onPause(){super.onPause();if(b!=null&&b.videoView.isPlaying())b.videoView.pause();}
    @Override protected void onDestroy(){super.onDestroy();if(b!=null)b.videoView.stopPlayback();}
}
