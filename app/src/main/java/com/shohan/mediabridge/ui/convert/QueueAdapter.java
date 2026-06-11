package com.shohan.mediabridge.ui.convert;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shohan.mediabridge.R;
import com.shohan.mediabridge.model.ConversionTask;
import java.io.File;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {
    public interface CancelListener { void onCancel(ConversionTask task); }
    private final Context ctx;
    private final List<ConversionTask> tasks;
    private CancelListener cancelListener;
    public void setCancelListener(CancelListener l) { cancelListener = l; }
    public QueueAdapter(Context c, List<ConversionTask> t) { ctx = c; tasks = t; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_queue, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        ConversionTask t = tasks.get(pos);
        h.tvFilename.setText(new File(t.inputPath).getName());
        h.tvFormat.setText(t.type + " \u2192 " + t.format);
        switch (t.status) {
            case "RUNNING":
                h.progressBar.setVisibility(View.VISIBLE);
                h.progressBar.setProgress(t.progress);
                h.btnCancel.setVisibility(View.VISIBLE);
                h.tvStatus.setText("\u23f3 " + t.progress + "%");
                h.tvStatus.setTextColor(0xFFAAAAAA);
                break;
            case "DONE":
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                String sz = t.outputSize > 0 ? " (" + (t.outputSize/1024) + " KB)" : "";
                h.tvStatus.setText("\u2705 Done" + sz);
                h.tvStatus.setTextColor(0xFF4CAF50);
                break;
            case "FAILED":
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                h.tvStatus.setText("\u274c Failed");
                h.tvStatus.setTextColor(0xFFFF5555);
                break;
            case "CANCELLED":
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                h.tvStatus.setText("\ud83d\udeab Cancelled");
                h.tvStatus.setTextColor(0xFFFFAA00);
                break;
            default:
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                h.tvStatus.setText("\u231b Waiting...");
                h.tvStatus.setTextColor(0xFFAAAAAA);
        }
        h.btnCancel.setOnClickListener(v -> { if (cancelListener != null) cancelListener.onCancel(t); });
    }

    @Override public int getItemCount() { return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvFilename, tvFormat, tvStatus;
        ProgressBar progressBar;
        ImageButton btnCancel;
        VH(View v) {
            super(v);
            tvFilename  = v.findViewById(R.id.tv_filename);
            tvFormat    = v.findViewById(R.id.tv_format);
            tvStatus    = v.findViewById(R.id.tv_status);
            progressBar = v.findViewById(R.id.progress_bar);
            btnCancel   = v.findViewById(R.id.btn_cancel);
        }
    }
}
