package com.theflexproject.thunder.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.theflexproject.thunder.model.MediaItem;

public abstract class BaseFragment extends Fragment {

    protected FragmentActivity mActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            mActivity = (FragmentActivity) context;
        }
    }

    protected void onNewIntent(Intent intent) {
    }

}
