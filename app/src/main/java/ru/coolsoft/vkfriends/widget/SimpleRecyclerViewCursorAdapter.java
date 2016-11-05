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
public class SimpleRecyclerViewCursorAdapter
extends RecyclerViewCursorAdapter<SimpleRecyclerViewCursorAdapterViewHolder>
implements View.OnClickListener{
    private String mFrom[][];
    private List<Map<String, Integer>> mFromIndexMaps = new ArrayList<>();
    private int mTo[][];
    private int mViewTypeIDs[];

    protected SimpleRecyclerViewCursorAdapter(){
        super();
    }

    public SimpleRecyclerViewCursorAdapter(Cursor cursor, String from[][], int to[][]
            , int... viewTypeIdLayoutResourceIDs) {
        this();
        init(cursor, from, to, viewTypeIdLayoutResourceIDs);
    }

    protected void init(Cursor cursor, String from[][], int to[][]
            , int... viewTypeIdLayoutResourceIDs){
        mFrom = from;
        mTo = to;
        mViewTypeIDs = viewTypeIdLayoutResourceIDs;

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
        view.setOnClickListener(this);

        return new SimpleRecyclerViewCursorAdapterViewHolder(view, mFrom[viewType], mTo[viewType]);
    }

    /**Default implementation is empty
     *
     * @param v root view of the list item being clicked
     */
    @Override
    public void onClick(View v) {
    }

    @Override
    protected void onBindViewHolder(SimpleRecyclerViewCursorAdapterViewHolder holder, Cursor cursor, int viewType) {
        for (int i = 0; i < mFrom[viewType].length && i < mTo[viewType].length; i++) {
            final String field = mFrom[viewType][i];
            final String value = cursor.getString(mFromIndexMaps.get(viewType).get(field));
            View view = holder.getViewByCursorField(field);
            if (view instanceof TextView){
                updateTextView(value, (TextView) view);
            } else if (view instanceof ImageView){
                updateImageView(value, (ImageView) view);
            } else {
                view.setTag(value);
            }
        }
    }

    /**
     * Called when a data string for the specified {@code view} is ready.
     * Default implementation puts the {@code value} as the {@code view} text
     *
     * @param value the string data selected for the specified {@code view}
     * @param view the text view to hold the selected {@code value}
     */
    protected void updateTextView(String value, TextView view) {
        view.setText(value);
    }

    /**
     * Called when a data string for the specified {@code view} is ready.
     * Default implementation puts the {@code value} as a source image for the {@code view}
     *
     * @param value the string data selected for the specified {@code view}
     * @param view the image view to hold the selected {@code value}
     */
    protected void updateImageView(String value, ImageView view) {
        view.setImageURI(Uri.parse(value));
    }
}
