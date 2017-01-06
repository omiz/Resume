package com.bemaxnet.resume.ui;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bemaxnet.resume.R;
import com.bemaxnet.resume.adapter.recyclerview.Adapter;
import com.bemaxnet.resume.adapter.recyclerview.ParentItem;
import com.bemaxnet.resume.adapter.recyclerview.ParentViewHolder;
import com.bemaxnet.resume.listener.ClickListener;
import com.bemaxnet.resume.listener.RecyclerTouchListener;
import com.bemaxnet.resume.resource.ResourcesHelper;
import com.bemaxnet.resume.ui.base.BaseFragment;
import com.bemaxnet.resume.ui.helper.ZSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ContentFragment extends BaseFragment implements ClickListener {

    public static String TAG = ContentFragment.class.getSimpleName();

    public static String INDEX_KEY = TAG + "INDEX_KEY";

    private static int DELAY = 1000;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(android.R.id.empty)
    View empty;

    Adapter adapter;

    private List<ParentItem> list = new ArrayList<>();

    private Unbinder unbinder;

    public int index;

    Handler handler;

    Runnable runnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);
        unbinder = ButterKnife.bind(this, view);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            index = bundle.getInt(INDEX_KEY, -1);
        }

        init();

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void init() {
        list.clear();
        list.addAll(ResourcesHelper.items(getContext(), index));

        adapter = new Adapter(getContext(), list);
        adapter.setEmptyView(empty);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(500);
        animator.setChangeDuration(500);
        animator.setRemoveDuration(500);
        animator.setSupportsChangeAnimations(true);
        recyclerView.setItemAnimator(animator);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, this));
    }

    @Override
    public String getTitle(Context context) {
        return index > -1 ? ResourcesHelper.tabsTitles(context)[index] : "";
    }

    @Override
    public void onClick(View view, int position) {
        Log.d(TAG, "sdfsdf" + position);
    }

    @Override
    public void onLongClick(View view, int position) {

    }

    @Override
    public void selected() {
        if (handler != null)
            handler.postDelayed(runnable, DELAY);
    }

    @Override
    public void deselected() {
        if (handler != null)
            handler.removeCallbacks(runnable);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        adapter.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        adapter.onRestoreInstanceState(savedInstanceState);
    }
}
