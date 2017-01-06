package com.bemaxnet.resume.adapter.recyclerview;

import com.bignerdranch.expandablerecyclerview.model.Parent;

import java.util.List;

public class ParentItem implements Parent<ChildItem> {
    public int image;
    public String title;
    public String subtitle;

    public List<ChildItem> childItems;

    @Override
    public List<ChildItem> getChildList() {
        return childItems;
    }

    @Override
    public boolean isInitiallyExpanded() {
        return false;
    }
}
