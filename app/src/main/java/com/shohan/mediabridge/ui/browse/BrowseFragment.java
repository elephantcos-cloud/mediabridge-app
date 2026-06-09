package com.shohan.mediabridge.ui.browse;
import android.content.Context;import android.content.Intent;
import android.database.Cursor;import android.os.Bundle;
import android.os.Handler;import android.os.Looper;
import android.provider.MediaStore;import android.view.*;
import androidx.annotation.*;import androidx.fragment.app.Fragment;
import com.google.android.material.tabs.TabLayoutMediator;
import com.shohan.mediabridge.databinding.FragmentBrowseBinding;
import com.shohan.mediabridge.model.MediaItem;
import com.shohan.mediabridge.player.*;import com.shohan.mediabridge.viewer.*;
import java.util.*;import java.util.concurrent.*;

public class BrowseFragment extends Fragment {
    private FragmentBrowseBinding b;
    private final ExecutorService pool=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private List<MediaItem> all=new ArrayList<>(),videos=new ArrayList<>(),
                            audios=new ArrayList<>(),images=new ArrayList<>();

    @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup p,Bundle s){
        b=FragmentBrowseBinding.inflate(i,p,false); return b.getRoot();}

    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        super.onViewCreated(v,s);
        MediaPagerAdapter adp=new MediaPagerAdapter(this);
        b.viewPager.setAdapter(adp);
        new TabLayoutMediator(b.tabLayout,b.viewPager,(tab,pos)->{
            String[]labels={"All","Video","Audio","Image"};
            tab.setText(labels[pos]);
        }).attach();
        loadMedia();
    }

    public void loadMedia(){
        if(b==null)return;
        b.progressBar.setVisibility(View.VISIBLE);
        pool.execute(()->{
            Context ctx=requireContext().getApplicationContext();
            videos=scanVideos(ctx); audios=scanAudio(ctx); images=scanImages(ctx);
            all=new ArrayList<>(); all.addAll(videos); all.addAll(audios); all.addAll(images);
            all.sort((a,x)->Long.compare(x.getDateAdded(),a.getDateAdded()));
            ui.post(()->{if(b!=null)b.progressBar.setVisibility(View.GONE);
                if(b!=null&&b.viewPager.getAdapter()!=null)b.viewPager.getAdapter().notifyDataSetChanged();});
        });
    }

    public List<MediaItem> getItemsForTab(int tab){
        switch(tab){case 1:return videos;case 2:return audios;case 3:return images;default:return all;}}

    public void openItem(MediaItem item){
        Intent intent;
        if(item.getType()==MediaItem.TYPE_VIDEO){
            intent=new Intent(requireContext(),VideoPlayerActivity.class);
        } else if(item.getType()==MediaItem.TYPE_AUDIO){
            intent=new Intent(requireContext(),AudioPlayerActivity.class);
        } else {
            intent=new Intent(requireContext(),ImageViewerActivity.class);
        }
        intent.putExtra("path",item.getPath());
        intent.putExtra("title",item.getName());
        startActivity(intent);
    }

    private List<MediaItem> scanVideos(Context ctx){
        List<MediaItem> list=new ArrayList<>();
        String[]proj={MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED};
        try(Cursor c=ctx.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                proj,null,null,MediaStore.Video.Media.DATE_ADDED+" DESC")){
            if(c!=null)while(c.moveToNext()){
                String path=c.getString(2);
                if(path!=null)list.add(new MediaItem(c.getLong(0),c.getString(1),path,
                    c.getLong(3),c.getLong(4),MediaItem.TYPE_VIDEO,c.getString(5),c.getLong(6)));
            }}catch(Exception e){}
        return list;
    }
    private List<MediaItem> scanAudio(Context ctx){
        List<MediaItem> list=new ArrayList<>();
        String[]proj={MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED};
        try(Cursor c=ctx.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj,null,null,MediaStore.Audio.Media.DATE_ADDED+" DESC")){
            if(c!=null)while(c.moveToNext()){
                String path=c.getString(2);
                if(path!=null)list.add(new MediaItem(c.getLong(0),c.getString(1),path,
                    c.getLong(3),c.getLong(4),MediaItem.TYPE_AUDIO,c.getString(5),c.getLong(6)));
            }}catch(Exception e){}
        return list;
    }
    private List<MediaItem> scanImages(Context ctx){
        List<MediaItem> list=new ArrayList<>();
        String[]proj={MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,MediaStore.Images.Media.DATE_ADDED};
        try(Cursor c=ctx.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj,null,null,MediaStore.Images.Media.DATE_ADDED+" DESC")){
            if(c!=null)while(c.moveToNext()){
                String path=c.getString(2);
                if(path!=null)list.add(new MediaItem(c.getLong(0),c.getString(1),path,
                    c.getLong(3),0,MediaItem.TYPE_IMAGE,c.getString(4),c.getLong(5)));
            }}catch(Exception e){}
        return list;
    }
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
