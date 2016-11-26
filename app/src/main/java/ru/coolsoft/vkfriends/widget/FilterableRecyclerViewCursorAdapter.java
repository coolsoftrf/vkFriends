package ru.coolsoft.vkfriends.widget;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

import java.util.ArrayList;

import ru.coolsoft.vkfriends.common.FriendsData;

/**
 * Created by Bobby© on 01.11.2016.
 * RecyclerView cursor adapter class with capability to filter the list of items
 * using the specified {@link java.lang.String} pattern
 */
public abstract class FilterableRecyclerViewCursorAdapter extends SimpleRecyclerViewCursorAdapter {
    private static class Change{
        public enum Type{
            Insertion,
            Modification,
            Removal
        }

        public Type _type;
        public int _from;
        public int _to;

        public Change(Type type, int from, int to){
            _type = type;
            _from = from;
            _to = to;
        }
    }

    private int[] mFieldIndicesToFilter;
    private MatrixCursor mMCursor;

    private int mIdFieldIndex = FriendsData.Invalid.INDEX;
    private ArrayList<String/*_id*/> mItems;

    private ArrayList<Change> mChanges;

    public FilterableRecyclerViewCursorAdapter(Cursor cursor, String[][] from, int[][] to
            , int[] filterableFieldIndices
            , IViewManagementDelegate delegate
            , int... viewTypeIdLayoutResourceIDs) {
        super();
        mFieldIndicesToFilter = filterableFieldIndices;
        init(cursor, from, to, delegate, viewTypeIdLayoutResourceIDs);
    }

    private static String[] getCursorValues(Cursor cursor) {
        int colCount = cursor.getColumnCount();
        String[] values = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            values[i] = cursor.getString(i);
        }
        return values;
    }

    private ArrayList<Change> changes(){
        if (mChanges == null){
            mChanges = new ArrayList<>();
        }
        return mChanges;
    }

    private void adjustRanges(Change.Type type, int position){
        final ArrayList<Change> cs = changes();
        if (cs.isEmpty()){
            return;
        }

        Change c = cs.get(cs.size() - 1);
        switch (type){
            case Insertion:
                if (c._type == Change.Type.Insertion && c._to == position - 1){
                    c._to++;
                } else {
                    mChanges.add(new Change(Change.Type.Insertion, position, position));
                }
                break;
            case Removal:
                if (c._type == Change.Type.Removal && c._from == position){
                    c._to++;
                } else {
                    mChanges.add(new Change(Change.Type.Removal, position, position));
                }
        }
    }

    /**
     * populate new matrix cursor with values of <b>original</b> cursor
     * matching the pattern in key fields with {@link #match(String[], String)}}
     * remembering ranges to add/remove at the Recycler View relative to <b>current</b> cursor
     * and finally swap cursors in the adapter closing old cursor if necessary
     *
     * @param pattern a String pattern to compare the Recycler View items against
     */
    public void filter(String pattern){
        if (mMCursor == null){
            return;
        }
        synchronized (this) {
            MatrixCursor newCursor = new MatrixCursor(mMCursor.getColumnNames());
            String[] keys = new String[mFieldIndicesToFilter.length];
            changes().clear();

            mMCursor.moveToPosition(-1);
            int index = 0;
            while (mMCursor.moveToNext()) {
                for (int i = 0; i < mFieldIndicesToFilter.length; i++) {
                    keys[i] = mMCursor.getString(mFieldIndicesToFilter[i]);
                }

                String id = mMCursor.getString(mIdFieldIndex);
                if (match(keys, pattern)) {
                    newCursor.addRow(getCursorValues(mMCursor));

                    if (!mItems.contains(id)) {
                        adjustRanges(Change.Type.Insertion, index);
                        mItems.add(id);
                    }
                    index++;
                } else if (mItems.contains(id)) {
                    adjustRanges(Change.Type.Removal, index);

                    mItems.remove(id);
                }
            }

            Cursor oldCursor = super.swapCursor(newCursor);
            if (oldCursor != mMCursor) {
                oldCursor.close();
            }
        }
    }

    public boolean match(String[] keys, String pattern){
        for (String key : keys) {
            if (key.toLowerCase().contains(pattern))
                return true;
        }
        return false;
    }

    @Override
    protected void notifyDataChanged() {
        if (changes().isEmpty())
        {
            notifyDataSetChanged();
            return;
        }
        for (Change change : changes()) {
            switch(change._type){
                case Insertion:
                    notifyItemRangeInserted(change._from, change._to - change._from);
                    break;
                case Modification:
                    notifyItemRangeChanged(change._from, change._to - change._from);
                    break;
                case Removal:
                    notifyItemRangeRemoved(change._from, change._to - change._from);
                    break;
            }
        }
    }

    /**
     * Swaps cursor and reinitializes the reference cursor to the passed {@code cursor}
     * @param cursor new {@link Cursor} for the adapter to switch to
     * @return old cursor
     */
    @Override
    public Cursor swapCursor(Cursor cursor) {
        synchronized (this) {
            changes().clear();

            mIdFieldIndex = cursor == null
                    ? FriendsData.Invalid.INDEX
                    : cursor.getColumnIndex(BaseColumns._ID);
            mItems = new ArrayList<>(cursor == null ? 0 : cursor.getCount());

            if (cursor == null) {
                mMCursor = null;
            } else {
                mMCursor = new MatrixCursor(cursor.getColumnNames(), cursor.getCount());
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    String[] values = getCursorValues(cursor);
                    mMCursor.addRow(values);

                    if (mIdFieldIndex != FriendsData.Invalid.INDEX) {
                        mItems.add(cursor.getString(mIdFieldIndex));
                    }
                }
            }
            return super.swapCursor(mMCursor);
        }
    }
}
