package com.shohan.mediabridge.ui.convert;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
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
        h.tvFilename.setText(t.getFileName());
        h.tvFormat.setText(t.type + " -> " + t.format);
        switch (t.status) {
            case "RUNNING":
                h.progressBar.setVisibility(View.VISIBLE);
                h.progressBar.setProgress(t.progress);
                h.btnCancel.setVisibility(View.VISIBLE);
                setDot(h.statusDot, 0xFFAA88FF);
                h.tvStatus.setText(t.progress + "%  converting...");
                h.tvStatus.setTextColor(0xFFAA88FF);
                break;
            case "DONE":
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                String sz = t.outputSize > 0 ? "  (" + (t.outputSize/1024) + " KB)" : "";
                setDot(h.statusDot, 0xFF4CAF50);
                h.tvStatus.setText("Done" + sz);
                h.tvStatus.setTextColor(0xFF4CAF50);
                break;
            case "FAILED":
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                setDot(h.statusDot, 0xFFFF5555);
                h.tvStatus.setText("Failed");
                h.tvStatus.setTextColor(0xFFFF5555);
                break;
            case "CANCELLED":
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                setDot(h.statusDot, 0xFFFFAA00);
                h.tvStatus.setText("Cancelled");
                h.tvStatus.setTextColor(0xFFFFAA00);
                break;
            default:
                h.progressBar.setVisibility(View.GONE);
                h.btnCancel.setVisibility(View.GONE);
                setDot(h.statusDot, 0xFF888888);
                h.tvStatus.setText("Waiting...");
                h.tvStatus.setTextColor(0xFF888888);
        }
        h.btnCancel.setOnClickListener(v -> { if (cancelListener != null) cancelListener.onCancel(t); });
    }

    private void setDot(View dot, int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        dot.setBackground(gd);
    }

    @Override public int getItemCount() { return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvFilename, tvFormat, tvStatus;
        ProgressBar progressBar;
        ImageButton btnCancel;
        View statusDot;
        VH(View v) {
            super(v);
            tvFilename  = v.findViewById(R.id.tv_filename);
            tvFormat    = v.findViewById(R.id.tv_format);
            tvStatus    = v.findViewById(R.id.tv_status);
            progressBar = v.findViewById(R.id.progress_bar);
            btnCancel   = v.findViewById(R.id.btn_cancel);
            statusDot   = v.findViewById(R.id.status_dot);
        }
    }
}
