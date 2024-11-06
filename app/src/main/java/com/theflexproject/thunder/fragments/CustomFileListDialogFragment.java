package com.theflexproject.thunder.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.FileItemDialogAdapter;
import com.theflexproject.thunder.model.MyMedia;

import java.util.List;

public class CustomFileListDialogFragment extends DialogFragment {

    Context context;
    RecyclerView fileListInDialog;
    List<MyMedia> mediaList;
    FileItemDialogAdapter.OnItemClickListener listener;
    public OnInputListener mOnInputListener;
    View source;

    public CustomFileListDialogFragment(Context context, View source, List<MyMedia> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
        this.source = source;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set style to make the dialog full-screen and modal
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Request to remove the title (if any) and make it fullscreen
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        return inflater.inflate(R.layout.fragment_custom_file_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listener = (view1, position) -> {
            if (mOnInputListener != null) {
                mOnInputListener.sendInput(position);
            }
            dismiss();
        };

        fileListInDialog = view.findViewById(R.id.fileListInDialog);
        fileListInDialog.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        fileListInDialog.setHasFixedSize(true);
        FileItemDialogAdapter fileItemDialogAdapter = new FileItemDialogAdapter(mediaList, listener);
        fileListInDialog.setAdapter(fileItemDialogAdapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mOnInputListener = (OnInputListener) getActivity();
        } catch (ClassCastException e) {
            System.out.println("onAttach: ClassCastException: " + e.getMessage());
        }
    }

    public interface OnInputListener {
        void sendInput(int selection);
    }
}
