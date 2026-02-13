package com.theflexproject.thunder.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.theflexproject.thunder.R;
import com.theflexproject.thunder.adapter.FragmentHomeAdapter;

public class HomeNewFragment extends BaseFragment {

    TabLayout tabLayout;
    TabItem moviesTab;
    TabItem tvTab;
    ViewPager2 viewPagerLibrary;
    FragmentHomeAdapter fragmentViewPagerAdapter;

    public HomeNewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        android.util.Log.d("HomeFragmentDebug", "HomeNewFragment onViewCreated called");
        initWidgets();

        // Initialize TabLayout and ViewPager2 after initWidgets()
        tabLayout = view.findViewById(R.id.tabLayout2);
        viewPagerLibrary = view.findViewById(R.id.homePagerLibrary);

        moviesTab = view.findViewById(R.id.movieTab2);
        tvTab = view.findViewById(R.id.seriesTab);

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

        // Disable user swiping in the ViewPager2
        viewPagerLibrary.setUserInputEnabled(false);
    }

    private void initWidgets() {
        viewPagerLibrary = mActivity.findViewById(R.id.homePagerLibrary);
        fragmentViewPagerAdapter = new FragmentHomeAdapter(this);
        viewPagerLibrary.setSaveEnabled(false);
        viewPagerLibrary.setAdapter(fragmentViewPagerAdapter);
    }
}
