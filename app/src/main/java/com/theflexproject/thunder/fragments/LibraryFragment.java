package com.theflexproject.thunder.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.FragmentViewPagerAdapter;

public class LibraryFragment extends BaseFragment {




    AutoCompleteTextView autoCompleteTextView;
    String[] sort_methods;
    ArrayAdapter<String> arrayAdapter;

    TabLayout tabLayout ;
    TabItem moviesTab;
    TabItem tvTab;
    TabItem filesTab;
    ViewPager2 viewPagerLibrary;
    FragmentViewPagerAdapter fragmentViewPagerAdapter;

    public LibraryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initWidgets();
        tabLayout = mActivity.findViewById(R.id.tabLayout);
        moviesTab = mActivity.findViewById(R.id.movieTab);
        tvTab = mActivity.findViewById(R.id.tvTab);
        filesTab = mActivity.findViewById(R.id.filesTab);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPagerLibrary.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewPagerLibrary.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        viewPagerLibrary.setUserInputEnabled(false);

    }

    private void initWidgets() {

        tabLayout = mActivity.findViewById(R.id.tabLayout);
        viewPagerLibrary = mActivity.findViewById(R.id.viewPagerLibrary);
        fragmentViewPagerAdapter = new FragmentViewPagerAdapter(this);
        viewPagerLibrary.setSaveEnabled(false);
        viewPagerLibrary.setAdapter(fragmentViewPagerAdapter);


    }
}