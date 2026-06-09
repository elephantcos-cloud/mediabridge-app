package com.shohan.mediabridge.ui.history;
import android.content.Context;import android.view.*;import android.widget.*;
import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;
import com.shohan.mediabridge.R;import com.shohan.mediabridge.db.ConversionRecord;
import com.shohan.mediabridge.utils.FileUtils;
import java.io.File;import java.util.List;
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
    public interface AL{void onAction(ConversionRecord r,String act);}
    private final Context ctx;private List<ConversionRecord> items;private final AL al;
    public HistoryAdapter(Context c,List<ConversionRecord> i,AL l){ctx=c;items=i;al=l;}
    public void setItems(List<ConversionRecord> l){items=l;notifyDataSetChanged();}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_history,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int pos){
        ConversionRecord r=items.get(pos);
        h.name.setText(r.sourceName);
        h.fmt.setText(r.mediaType+" \u2192 "+r.outputFormat);
        h.date.setText(FileUtils.fmtDate(r.timestamp));
        boolean ex=r.outputPath!=null&&new File(r.outputPath).exists();
        h.size.setText(r.outputSize>0?FileUtils.fmtSize(r.outputSize):(ex?"Exists":"Not found"));
        boolean ok="SUCCESS".equals(r.status);
        h.status.setText(ok?"\u2705 Success":"\u274c Failed");
        h.status.setTextColor(ctx.getResources().getColor(ok?android.R.color.holo_green_light:android.R.color.holo_red_light));
        h.btnOpen.setEnabled(ex);
        h.btnOpen.setOnClickListener(v->al.onAction(r,"open"));
        h.btnDel.setOnClickListener(v->al.onAction(r,"delete"));
    }
    @Override public int getItemCount(){return items.size();}
    static class VH extends RecyclerView.ViewHolder{
        TextView name,fmt,date,size,status;ImageButton btnOpen,btnDel;
        VH(View v){super(v);name=v.findViewById(R.id.tv_name);fmt=v.findViewById(R.id.tv_format);
            date=v.findViewById(R.id.tv_date);size=v.findViewById(R.id.tv_size);
            status=v.findViewById(R.id.tv_status);
            btnOpen=v.findViewById(R.id.btn_open);btnDel=v.findViewById(R.id.btn_delete);}}
}
