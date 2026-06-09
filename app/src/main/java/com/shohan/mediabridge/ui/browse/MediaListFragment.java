package com.shohan.mediabridge.ui.browse;
import android.os.Bundle;import android.view.*;
import androidx.annotation.*;import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;import androidx.recyclerview.widget.RecyclerView;
import com.shohan.mediabridge.model.MediaItem;
import java.util.*;
public class MediaListFragment extends Fragment {
    private static final String ARG="tab";
    private int tab;
    public static MediaListFragment newInstance(int tab){
        MediaListFragment f=new MediaListFragment();
        Bundle b=new Bundle();b.putInt(ARG,tab);f.setArguments(b);return f;}
    @Override public void onCreate(@Nullable Bundle s){super.onCreate(s);
        tab=(getArguments()!=null)?getArguments().getInt(ARG):0;}
    @Override public View onCreateView(@NonNull LayoutInflater inf,ViewGroup parent,Bundle s){
        RecyclerView rv=new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(0,8,0,80);rv.setClipToPadding(false);
        BrowseFragment bf=getParentBf();
        List<MediaItem> items=(bf!=null)?bf.getItemsForTab(tab):new ArrayList<>();
        MediaAdapter adp=new MediaAdapter(requireContext(),items,item->{if(bf!=null)bf.openItem(item);});
        rv.setAdapter(adp);return rv;}
    private BrowseFragment getParentBf(){Fragment f=getParentFragment();return (f instanceof BrowseFragment)?(BrowseFragment)f:null;}
}
