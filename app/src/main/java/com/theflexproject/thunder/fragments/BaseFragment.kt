package com.theflexproject.thunder.fragments

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

abstract class BaseFragment : Fragment() {

    @JvmField
    protected var mActivity: FragmentActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Activity) {
            mActivity = context as FragmentActivity
        }
    }
}
