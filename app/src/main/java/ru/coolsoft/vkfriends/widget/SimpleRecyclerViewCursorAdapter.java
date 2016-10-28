package ru.coolsoft.vkfriends.widget;

import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bobby© on 23.10.2016.
 * Simple RecyclerView adapter that maps fields of the given cursor
 * enumerated in {@code from} constructor parameter
 * into views with IDs enumerated in {@code to} parameter
 * of the view holders created according to IDs enumerated in {@code viewTypeIDs}
 */
public class SimpleRecyclerViewCursorAdapter extends RecyclerViewCursorAdapter<SimpleRecyclerViewCursorAdapterViewHolder> {
    String mFrom[][];
    List<Map<String, Integer>> mFromIndexMaps = new ArrayList<>();
    int mTo[][];
    int mViewTypeIDs[];
    View.OnClickListener mListener;

    public SimpleRecyclerViewCursorAdapter(Cursor cursor, String from[][], int to[][]
            , int viewTypeIdLayoutResourceIDs[], View.OnClickListener listener) {
        super(null);

        mFrom = from;
        mTo = to;
        mViewTypeIDs = viewTypeIdLayoutResourceIDs;
        mListener = listener;

        swapCursor(cursor);
    }

    @Override
    protected void updateCursorFields(Cursor cursor) {
        for (String[] fields : mFrom) {
            final Map<String, Integer> map = new HashMap<>();
            for (String field : fields) {
                map.put(field, cursor.getColumnIndex(field));
            }
            mFromIndexMaps.add(map);
        }
    }

    @Override
    public SimpleRecyclerViewCursorAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType >= mViewTypeIDs.length) {
            return null;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(mViewTypeIDs[viewType], parent, false);

        if (mListener != null) {
            view.setOnClickListener(mListener);
        }

        return new SimpleRecyclerViewCursorAdapterViewHolder(view, mFrom[viewType], mTo[viewType]);
    }

    @Override
    protected void onBindViewHolder(SimpleRecyclerViewCursorAdapterViewHolder holder, Cursor cursor, int viewType) {
        for (int i = 0; i < mFrom[viewType].length && i < mTo[viewType].length; i++) {
            final String field = mFrom[viewType][i];
            final String value = cursor.getString(mFromIndexMaps.get(viewType).get(field));
            View view = holder.getViewByCursorField(field);
            if (view instanceof TextView){
                ((TextView)view).setText(value);
            } else if (view instanceof ImageView){
                ((ImageView)view).setImageURI(Uri.parse(value));
            } else {
                view.setTag(value);
            }
        }
    }
}
