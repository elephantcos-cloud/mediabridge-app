package com.shohan.mediabridge.ui.browse;
import android.content.Context;import android.content.Intent;import android.database.Cursor;
import android.os.Bundle;import android.os.Handler;import android.os.Looper;
import android.provider.MediaStore;import android.view.*;import android.widget.Toast;
import androidx.annotation.*;import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.shohan.mediabridge.databinding.FragmentBrowseBinding;
import com.shohan.mediabridge.model.MediaItem;
import com.shohan.mediabridge.player.AudioPlayerActivity;
import com.shohan.mediabridge.player.VideoPlayerActivity;
import com.shohan.mediabridge.viewer.ImageViewerActivity;
import java.util.*;import java.util.concurrent.*;import java.util.stream.Collectors;
public class BrowseFragment extends Fragment {
    private FragmentBrowseBinding b;
    private MediaAdapter adapter;
    private final List<MediaItem> allItems=new ArrayList<>();
    private int filter=-1;
    private final ExecutorService pool=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    @Override public View onCreateView(@NonNull LayoutInflater i,ViewGroup p,Bundle s){
        b=FragmentBrowseBinding.inflate(i,p,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        super.onViewCreated(v,s);
        adapter=new MediaAdapter(requireContext(),new ArrayList<>(),this::openItem);
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);
        b.chipAll.setOnClickListener(x->applyFilter(-1));
        b.chipVideo.setOnClickListener(x->applyFilter(0));
        b.chipAudio.setOnClickListener(x->applyFilter(1));
        b.chipImage.setOnClickListener(x->applyFilter(2));
        loadMedia();}
    public void loadMedia(){
        if(!isAdded())return;
        final Context ctx=requireContext().getApplicationContext();
        ui.post(()->{if(b!=null)b.progressBar.setVisibility(android.view.View.VISIBLE);});
        pool.execute(()->{
            List<MediaItem> loaded=new ArrayList<>();
            loaded.addAll(scan(ctx,"video"));loaded.addAll(scan(ctx,"audio"));loaded.addAll(scan(ctx,"image"));
            loaded.sort((a,x)->Long.compare(x.getDateAdded(),a.getDateAdded()));
            ui.post(()->{if(b==null)return;allItems.clear();allItems.addAll(loaded);
                b.progressBar.setVisibility(android.view.View.GONE);applyFilter(filter);});}); }
    private void applyFilter(int type){
        filter=type;if(b==null)return;
        List<MediaItem> f=(type==-1)?new ArrayList<>(allItems):
            allItems.stream().filter(i->i.getType()==type).collect(Collectors.toList());
        adapter.setItems(f);
        b.chipAll.setSelected(type==-1);b.chipVideo.setSelected(type==0);
        b.chipAudio.setSelected(type==1);b.chipImage.setSelected(type==2);}
    public List<MediaItem> getItemsForTab(int tab){
        if(tab==0)return allItems.stream().filter(i->i.getType()==MediaItem.TYPE_VIDEO).collect(Collectors.toList());
        if(tab==1)return allItems.stream().filter(i->i.getType()==MediaItem.TYPE_AUDIO).collect(Collectors.toList());
        if(tab==2)return allItems.stream().filter(i->i.getType()==MediaItem.TYPE_IMAGE).collect(Collectors.toList());
        return new ArrayList<>(allItems);}
    public void openItem(MediaItem item){
        if(!isAdded())return;
        Intent intent;
        if(item.getType()==MediaItem.TYPE_VIDEO)intent=new Intent(requireContext(),VideoPlayerActivity.class);
        else if(item.getType()==MediaItem.TYPE_AUDIO)intent=new Intent(requireContext(),AudioPlayerActivity.class);
        else intent=new Intent(requireContext(),ImageViewerActivity.class);
        intent.putExtra("path",item.getPath());intent.putExtra("title",item.getName());
        try{startActivity(intent);}catch(Exception e){
            Toast.makeText(requireContext(),"Cannot open file",Toast.LENGTH_SHORT).show();}}
    private List<MediaItem> scan(Context ctx,String kind){
        List<MediaItem> list=new ArrayList<>();
        android.net.Uri uri;String[] proj;int type;
        if("video".equals(kind)){uri=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            proj=new String[]{MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_ADDED};type=MediaItem.TYPE_VIDEO;
        }else if("audio".equals(kind)){uri=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            proj=new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATE_ADDED};type=MediaItem.TYPE_AUDIO;
        }else{uri=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            proj=new String[]{MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,MediaStore.Images.Media.SIZE,
                "0",MediaStore.Images.Media.MIME_TYPE,MediaStore.Images.Media.DATE_ADDED};
            type=MediaItem.TYPE_IMAGE;}
        try(Cursor c=ctx.getContentResolver().query(uri,proj,null,null,proj[6]+" DESC")){
            if(c!=null)while(c.moveToNext()){String path=c.getString(2);
                if(path!=null)list.add(new MediaItem(c.getLong(0),c.getString(1),path,
                    c.getLong(3),c.getLong(4),type,c.getString(5),c.getLong(6)));
            }}catch(Exception ignored){}
        return list;}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
