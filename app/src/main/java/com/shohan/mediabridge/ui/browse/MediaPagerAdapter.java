package com.shohan.mediabridge.ui.browse;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
public class MediaPagerAdapter extends FragmentStateAdapter {
    public MediaPagerAdapter(@NonNull BrowseFragment f){super(f);}
    @NonNull @Override public Fragment createFragment(int pos){return MediaListFragment.newInstance(pos);}
    @Override public int getItemCount(){return 4;}
}
