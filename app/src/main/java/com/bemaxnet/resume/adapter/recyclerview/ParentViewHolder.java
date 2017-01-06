package com.bemaxnet.resume.adapter.recyclerview;

import android.support.annotation.NonNull;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bemaxnet.resume.R;

public class ParentViewHolder extends com.bignerdranch.expandablerecyclerview.ParentViewHolder {
    public ImageView imageView;
    public TextView title, subtitle;

    public ParentViewHolder(View view) {
        super(view);
        imageView = (ImageView) view.findViewById(R.id.image);
        title = (TextView) view.findViewById(R.id.title);
        subtitle = (TextView) view.findViewById(R.id.subtitle);

        title.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(title, Linkify.ALL);

        subtitle.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(subtitle, Linkify.ALL);
    }

    public void bind(@NonNull ParentItem item) {
        if (item.image > 0) {
            imageView.setImageResource(item.image);
        } else {
            imageView.setImageBitmap(null);
        }

        imageView.setVisibility(item.image > 0 ? View.VISIBLE : View.GONE);

        title.setText(item.title);
        subtitle.setText(item.subtitle);
        subtitle.setText(item.subtitle);
    }
}