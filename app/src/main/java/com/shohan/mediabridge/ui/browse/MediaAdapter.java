package com.shohan.mediabridge.ui.browse;
import android.content.Context;import android.view.*;import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.shohan.mediabridge.R;import com.shohan.mediabridge.model.MediaItem;
import java.util.List;
public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.VH> {
    public interface Click{void onClick(MediaItem item);}
    private final Context ctx; private final List<MediaItem> items; private final Click click;
    public MediaAdapter(Context c,List<MediaItem> i,Click cl){ctx=c;items=i;click=cl;}
    public void setItems(List<MediaItem> newItems){
        items.clear();items.addAll(newItems);notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_media,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int pos){
        MediaItem item=items.get(pos);
        h.title.setText(item.getName());
        String sub=item.getFormattedSize();
        if(!item.getFormattedDuration().isEmpty())sub+="\u2003\u2022\u2003"+item.getFormattedDuration();
        h.sub.setText(sub);
        switch(item.getType()){
            case MediaItem.TYPE_VIDEO:h.icon.setImageResource(R.drawable.ic_video);break;
            case MediaItem.TYPE_AUDIO:h.icon.setImageResource(R.drawable.ic_audio);break;
            default:h.icon.setImageResource(R.drawable.ic_image);break;}
        if(item.getType()!=MediaItem.TYPE_AUDIO){
            Glide.with(ctx).load(item.getPath()).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .placeholder(R.drawable.ic_image).error(R.drawable.ic_image)
                .centerCrop().into(h.thumb);
        } else h.thumb.setImageResource(R.drawable.ic_audio);
        h.itemView.setOnClickListener(v->click.onClick(item));
    }
    @Override public int getItemCount(){return items.size();}
    static class VH extends RecyclerView.ViewHolder{
        ImageView thumb,icon;TextView title,sub;
        VH(View v){super(v);thumb=v.findViewById(R.id.thumbnail);icon=v.findViewById(R.id.type_icon);
            title=v.findViewById(R.id.title);sub=v.findViewById(R.id.subtitle);}}
}
