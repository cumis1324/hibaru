package com.theflexproject.thunder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.model.DownloadItem;

import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {
    private List<DownloadItem> downloadItems;
    private Context context;
    private boolean isTV;
    private OnCancelListener onCancelListener;

    public interface OnCancelListener {
        void onCancel(DownloadItem item);
    }

    public DownloadAdapter(List<DownloadItem> downloadItems, Context context, boolean isTV, OnCancelListener listener) {
        this.downloadItems = downloadItems;
        this.context = context;
        this.isTV = isTV;
        this.onCancelListener = listener;
    }

    // Backward compatibility constructor
    public DownloadAdapter(List<DownloadItem> downloadItems, Context context) {
        this(downloadItems, context, false, null);
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isTV ? R.layout.downloading_item_tv : R.layout.downloading_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = downloadItems.get(position);
        String displayName = item.getFilename();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = item.getTitle();
        }
        holder.tvFileName.setText(displayName);
        holder.progressBar.setProgress(item.getProgress());

        if (!isTV) {
            if (holder.tvDownloadPercent != null) {
                holder.tvDownloadPercent.setText(item.getProgress() + "%");
            }
            if (holder.tvDownloadStatus != null) {
                // Determine status text based on progress or other state if available
                String status = "Downloading...";
                if (item.getProgress() >= 100)
                    status = "Finalizing...";
                holder.tvDownloadStatus.setText(status);
            }
            if (holder.btnCancel != null) {
                holder.btnCancel.setOnClickListener(v -> {
                    if (onCancelListener != null) {
                        onCancelListener.onCancel(item);
                    }
                });
            }
        }
        // ... (rest of the focus logic)

        if (isTV) {
            holder.itemView.setFocusable(true);
            holder.itemView.setFocusableInTouchMode(false);

            // Handle click to cancel on TV
            holder.itemView.setOnClickListener(v -> {
                if (onCancelListener != null) {
                    onCancelListener.onCancel(item);
                }
            });

            // Handle focus for glow effect if needed, though OnItemViewSelectedListener in
            // Fragment is better for global preview
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                View overlay = v.findViewById(R.id.focusOverlay);
                if (overlay != null) {
                    overlay.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                    if (hasFocus) {
                        // Animation scale up
                        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start();
                    } else {
                        // Animation scale down
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return downloadItems.size();
    }

    public void updateProgress(long downloadId, int progress) {
        for (DownloadItem item : downloadItems) {
            if (item.getDownloadId() == downloadId) {
                item.setProgress(progress);
                notifyDataSetChanged();
                break;
            }
        }
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        ProgressBar progressBar;
        TextView tvDownloadStatus; // Mobile only
        TextView tvDownloadPercent; // Mobile only
        android.widget.ImageButton btnCancel; // Mobile only

        DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            progressBar = itemView.findViewById(R.id.progressDownload);
            tvDownloadStatus = itemView.findViewById(R.id.tvDownloadStatus);
            tvDownloadPercent = itemView.findViewById(R.id.tvDownloadPercent);
            btnCancel = itemView.findViewById(R.id.btnCancelDownload);
        }
    }
}
