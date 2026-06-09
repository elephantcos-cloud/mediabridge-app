package com.shohan.mediabridge.player;
import android.animation.ValueAnimator;import android.media.MediaPlayer;
import android.os.Bundle;import android.os.Handler;import android.os.Looper;
import android.view.animation.LinearInterpolator;import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import com.shohan.mediabridge.databinding.ActivityAudioPlayerBinding;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;import java.io.IOException;
public class AudioPlayerActivity extends AppCompatActivity {
    private ActivityAudioPlayerBinding b;
    private MediaPlayer mp;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean playing=false;
    private ValueAnimator disc;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(mp!=null&&mp.isPlaying()){
            int cur=mp.getCurrentPosition();
            b.seekBar.setProgress(cur);
            b.tvCurrent.setText(FileUtils.fmtDuration(cur));
            handler.postDelayed(this,500);}}};
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        b=ActivityAudioPlayerBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        String path=getIntent().getStringExtra("path");
        String title=getIntent().getStringExtra("title");
        b.tvTitle.setText(title!=null?title:"Audio");
        b.btnBack.setOnClickListener(v->finish());
        b.btnPlayPause.setOnClickListener(v->toggle());
        b.btnRewind.setOnClickListener(v->{if(mp!=null)mp.seekTo(Math.max(0,mp.getCurrentPosition()-10000));});
        b.btnForward.setOnClickListener(v->{if(mp!=null)mp.seekTo(Math.min(mp.getDuration(),mp.getCurrentPosition()+10000));});
        if(path==null||!new File(path).exists()){b.tvTitle.setText("File not found");return;}
        initPlayer(path);spinDisc();
    }
    private void initPlayer(String path){
        mp=new MediaPlayer();
        try{mp.setDataSource(path);mp.prepare();
            int dur=mp.getDuration();
            b.seekBar.setMax(dur);b.tvDuration.setText(FileUtils.fmtDuration(dur));b.tvCurrent.setText("0:00");
            b.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                @Override public void onProgressChanged(SeekBar sb,int prog,boolean user){
                    if(user){mp.seekTo(prog);b.tvCurrent.setText(FileUtils.fmtDuration(prog));}}
                @Override public void onStartTrackingTouch(SeekBar sb){}
                @Override public void onStopTrackingTouch(SeekBar sb){}});
            mp.setOnCompletionListener(m->{playing=false;
                b.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                handler.removeCallbacks(tick);});
            mp.start();playing=true;
            b.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            handler.post(tick);
        }catch(IOException e){b.tvTitle.setText("Cannot play: "+e.getMessage());}
    }
    private void toggle(){
        if(mp==null)return;
        if(playing){mp.pause();playing=false;b.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);handler.removeCallbacks(tick);}
        else{mp.start();playing=true;b.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);handler.post(tick);}
    }
    private void spinDisc(){
        disc=ValueAnimator.ofFloat(0f,360f);disc.setDuration(8000);
        disc.setRepeatCount(ValueAnimator.INFINITE);disc.setInterpolator(new LinearInterpolator());
        disc.addUpdateListener(a->b.discView.setRotation((float)a.getAnimatedValue()));disc.start();
    }
    @Override protected void onDestroy(){super.onDestroy();
        handler.removeCallbacks(tick);if(disc!=null)disc.cancel();
        if(mp!=null){mp.stop();mp.release();mp=null;}}
}
