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

    public DownloadAdapter(List<DownloadItem> downloadItems, Context context) {
        this.downloadItems = downloadItems;
        this.context = context;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.downloading_item, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = downloadItems.get(position);
        holder.tvFileName.setText(item.getFileName());
        holder.progressBar.setProgress(item.getProgress());
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

        DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            progressBar = itemView.findViewById(R.id.progressDownload);
        }
    }
}
