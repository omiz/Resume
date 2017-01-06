package com.bemaxnet.resume.adapter.recyclerview;

import android.support.annotation.NonNull;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bemaxnet.resume.R;

public class ChildViewHolder extends com.bignerdranch.expandablerecyclerview.ChildViewHolder {
    public TextView textView;

    public ChildViewHolder(View view) {
        super(view);

        textView = (TextView) view.findViewById(R.id.text);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
//        Linkify.addLinks(textView, Linkify.ALL);
    }

    public void bind(@NonNull ChildItem item) {
        textView.setText(item.note);
    }
}
