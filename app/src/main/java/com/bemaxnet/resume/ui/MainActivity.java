package com.bemaxnet.resume.ui;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bemaxnet.resume.R;
import com.bemaxnet.resume.adapter.MainFragmentPagerAdapter;
import com.bemaxnet.resume.ui.base.BaseFragment;
import com.gigamole.navigationtabstrip.NavigationTabStrip;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.tabStrip)
    NavigationTabStrip tabStrip;

    MainFragmentPagerAdapter adapter;

    int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setup();
    }

    private void setup() {
        adapter = new MainFragmentPagerAdapter(getSupportFragmentManager(), this);

        viewPager.setAdapter(adapter);

        tabStrip.setViewPager(viewPager);

        tabStrip.setTitles(adapter.getTitles());
    }

    @OnPageChange(R.id.viewPager)
    public void onPageScrollStateChanged(int index) {
        ((BaseFragment)adapter.getItem(this.index)).deselected();
        ((BaseFragment)adapter.getItem(index)).selected();
    }
}
