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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.FileItemDialogAdapter;
import com.theflexproject.thunder.model.MyMedia;

import java.util.List;

public class CustomFileListDialogFragment extends BottomSheetDialogFragment {

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

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

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
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            // Mengambil BottomSheet untuk mengatur state-nya
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            // Mengatur BottomSheet ke state expanded secara otomatis

            // Optional: Mengatur tinggi Bottom Sheet ke full-screen
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;

        }
    }
}
