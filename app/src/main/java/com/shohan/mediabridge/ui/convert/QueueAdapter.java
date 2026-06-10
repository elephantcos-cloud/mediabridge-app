package com.shohan.mediabridge.ui.convert;
import android.content.Context;import android.view.*;import android.widget.*;
import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;
import com.shohan.mediabridge.R;import com.shohan.mediabridge.model.ConversionTask;
import com.shohan.mediabridge.utils.FileUtils;import java.util.List;
public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {
    private final Context ctx; private final List<ConversionTask> tasks;
    public QueueAdapter(Context c,List<ConversionTask> t){ctx=c;tasks=t;}
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_queue,p,false));}
    @Override public void onBindViewHolder(@NonNull VH h,int pos){
        ConversionTask t=tasks.get(pos);
        h.name.setText(t.getFileName());
        h.fmt.setText(t.type+" \u2192 "+t.format);
        switch(t.status){
            case "WAITING":h.status.setText("Waiting...");h.status.setTextColor(0xFF9E9E9E);
                h.bar.setIndeterminate(false);h.bar.setProgress(0);h.pct.setText("0%");break;
            case "RUNNING":h.status.setText("Converting...");h.status.setTextColor(0xFF03DAC6);
                h.bar.setIndeterminate(t.progress==0);h.bar.setProgress(t.progress);
                h.pct.setText(t.progress+"%");break;
            case "DONE":h.status.setText("\u2705 Done  "+FileUtils.fmtSize(t.outputSize));
                h.status.setTextColor(0xFF4CAF50);h.bar.setIndeterminate(false);
                h.bar.setProgress(100);h.pct.setText("100%");break;
            case "FAILED":h.status.setText("\u274c Failed");h.status.setTextColor(0xFFEF5350);
                h.bar.setIndeterminate(false);h.bar.setProgress(0);h.pct.setText("--");break;
        }
    }
    @Override public int getItemCount(){return tasks.size();}
    static class VH extends RecyclerView.ViewHolder{
        TextView name,fmt,status,pct;ProgressBar bar;
        VH(View v){super(v);
            name=v.findViewById(R.id.tv_queue_name);fmt=v.findViewById(R.id.tv_queue_format);
            status=v.findViewById(R.id.tv_queue_status);pct=v.findViewById(R.id.tv_queue_pct);
            bar=v.findViewById(R.id.queue_progress);}}
}
