package com.bemaxnet.resume.adapter.recyclerview;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.bemaxnet.resume.R;
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter;

import java.util.List;
import java.util.Random;

/**
 * Created by zombie on 18/08/16.
 */
public class Adapter extends ExpandableRecyclerAdapter<ParentItem, ChildItem, ParentViewHolder, ChildViewHolder> {
    private final static int FADE_DURATION = 1000;

    private View emptyView;
    private Runnable runnable;
    private TextSwitcher textSwitcher;

    private LayoutInflater mInflater;

    public Adapter(Context context, @NonNull List<ParentItem> list) {
        super(list);

        mInflater = LayoutInflater.from(context);
    }

    @UiThread
    @NonNull
    @Override
    public ParentViewHolder onCreateParentViewHolder(@NonNull ViewGroup parentViewGroup, int viewType) {
        return new ParentViewHolder(mInflater.inflate(R.layout.listitem_parent, parentViewGroup, false));
    }

    @UiThread
    @NonNull
    @Override
    public ChildViewHolder onCreateChildViewHolder(@NonNull ViewGroup childViewGroup, int viewType) {
        return new ChildViewHolder(mInflater.inflate(R.layout.listitem_child, childViewGroup, false));
    }

    @UiThread
    @Override
    public void onBindParentViewHolder(@NonNull ParentViewHolder viewHolder, int parentPosition, @NonNull ParentItem recipe) {
        viewHolder.bind(recipe);
    }

    @UiThread
    @Override
    public void onBindChildViewHolder(@NonNull ChildViewHolder viewHolder, int parentPosition, int childPosition, @NonNull ChildItem ingredient) {
        viewHolder.bind(ingredient);
    }

    @Override
    public int getItemCount() {
        int count = super.getItemCount();
        if (emptyView != null) {
            emptyView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            alternatingEmptyText(count == 0);
        }
        return count;
    }

    synchronized private void alternatingEmptyText(boolean alternate) {
        if (alternate) {
            final String[] texts = emptyView.getContext().getResources().getStringArray(R.array.emptyList);
            if (runnable == null) {
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        textSwitcher.setText(texts[new Random().nextInt(texts.length)]);

                        emptyView.postDelayed(this, 6000);
                    }
                };
            }
            emptyView.getHandler().removeCallbacks(runnable);
            emptyView.postDelayed(runnable, 1000);
        } else {

            emptyView.getHandler().removeCallbacks(runnable);
        }
    }

    public void setEmptyView(final View emptyView) {
        this.emptyView = emptyView;
        textSwitcher = (TextSwitcher) emptyView.findViewById(R.id.textSwitcher);
        if (textSwitcher != null) {
            textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

                @Override
                public View makeView() {

                    // Create a new TextView
                    TextView t = new TextView(emptyView.getContext());
                    t.setGravity(Gravity.CENTER);
                    TextViewCompat.setTextAppearance(t, R.style.AppTheme);
                    t.setTypeface(null, Typeface.BOLD);
                    t.setTextSize(16);
                    return t;
                }
            });

            Animation in = AnimationUtils.loadAnimation(emptyView.getContext(),
                    android.R.anim.fade_in);
            Animation out = AnimationUtils.loadAnimation(emptyView.getContext(),
                    android.R.anim.fade_out);
            textSwitcher.setInAnimation(in);
            textSwitcher.setOutAnimation(out);
        }
    }
}
