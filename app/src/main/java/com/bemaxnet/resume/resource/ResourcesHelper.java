package com.bemaxnet.resume.resource;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ArrayRes;

import com.bemaxnet.resume.R;
import com.bemaxnet.resume.adapter.recyclerview.ChildItem;
import com.bemaxnet.resume.adapter.recyclerview.ParentItem;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ResourcesHelper {

    private static Resources resources(Context context) {
        return context.getResources();
    }

    private static String[] strings(Context context, @ArrayRes final int id) {
        return resources(context).getStringArray(id);
    }

    public static String[] tabsTitles(Context context) {

        return strings(context, R.array.tabs);
    }

    public static ArrayList<ParentItem> items(Context context, final int index) {
        ArrayList<ParentItem> arrayList = new ArrayList<>();

        final int id = getResId("_" + index, R.array.class);

        if (id < 0) {
            return arrayList;
        }

        String[] resArray = strings(context, id);

        for (String aResArray : resArray) {
            String[] resItem = aResArray.split("::");

            ParentItem parentItem = new ParentItem();

            parentItem.image = resItem.length > 0 ? getResId(resItem[0], R.mipmap.class) : -1;
            parentItem.title = resItem.length > 1 ? resItem[1] : "";
            parentItem.subtitle = resItem.length > 2 ? resItem[2] : "";
            parentItem.childItems = new ArrayList<>();

            for (int i = 3; i < resItem.length; i++) {
                ChildItem childItem = new ChildItem();

                childItem.note = resItem[i];

                parentItem.childItems.add(childItem);
            }

            arrayList.add(parentItem);
        }

        return arrayList;
    }

    private static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            return -1;
        }
    }
}
