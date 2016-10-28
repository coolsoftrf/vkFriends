package ru.coolsoft.vkfriends.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Bobby© on 23.10.2016.
 * Base View holder class to parameterize classes derived from Simple cursor adapter
 */
public class SimpleRecyclerViewCursorAdapterViewHolder extends RecyclerView.ViewHolder{

    private Map<String, View> mViewMap = new HashMap<>();

    public SimpleRecyclerViewCursorAdapterViewHolder(View itemView, String[] from, int[] to) {
        super(itemView);
        for (int i = 0; i < from.length && i < to.length; i++) {
            mViewMap.put(from[i], super.itemView.findViewById(to[i]));
        }
    }

    public View getViewByCursorField(String fieldName){
        return mViewMap.get(fieldName);
    }
}