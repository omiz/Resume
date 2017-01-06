package com.bemaxnet.resume.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.bemaxnet.resume.delegate.PageDelegate;
import com.bemaxnet.resume.resource.ResourcesHelper;
import com.bemaxnet.resume.ui.ContentFragment;
import com.bemaxnet.resume.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

import static com.bemaxnet.resume.ui.ContentFragment.INDEX_KEY;


public class MainFragmentPagerAdapter extends FragmentPagerAdapter {
    private List<BaseFragment> fragmentList = new ArrayList<>();

    private final Context context;

    public MainFragmentPagerAdapter(FragmentManager fm, Context context)
    {
        super(fm);

        this.context = context;

        init();
    }

    private void init() {
        String[] titles = ResourcesHelper.tabsTitles(context);

        for (int index = 0; index < titles.length; index++) {
            ContentFragment fragment = new ContentFragment();

            fragment.index = index;

            fragmentList.add(fragment);
        }
    }

    @Override
    public Fragment getItem(int position)
    {
        return fragmentList.get(position);
    }

    @Override
    public int getCount()
    {
        return fragmentList.size();
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        if (fragmentList.get(position) != null)
        {
            return fragmentList.get(position).getTitle(context);
        } else {
            return "" + position;
        }
    }

    public String[] getTitles() {
        String[] titles = new String[getCount()];

        for (int index = 0; index < getCount(); index++) {
            titles[index] = String.valueOf(getPageTitle(index));
        }

        return titles;
    }
}
