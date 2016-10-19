package ru.coolsoft.vkfriends.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.vk.sdk.api.model.VKApiUser;

import java.io.File;

import ru.coolsoft.vkfriends.FriendsData;
import ru.coolsoft.vkfriends.R;
import ru.coolsoft.vkfriends.dummy.DummyContent;
import ru.coolsoft.vkfriends.dummy.DummyContent.DummyItem;
import ru.coolsoft.vkfriends.loaders.DatabaseUserImageSource;
import ru.coolsoft.vkfriends.loaders.ImageLoader;

/**
 * A fragment representing a list of friends to compare in main activity
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class FriendListFragment extends Fragment {

    private static final String ARG_ROOT_USER = "root-user";
    private OnListFragmentInteractionListener mListener;

    private VKApiUser mCurrentUser;
    Handler mHandler = new Handler();

    private ProgressBar mPhotoWaiter;
    private ImageView mAvatar;
    private Toolbar mToolbar;

    LoaderManager.LoaderCallbacks<String> mUserPhotoLoaderCallback = new LoaderManager.LoaderCallbacks<String>() {
        //ToDo: extract to a common Delegate for all the loadable images
        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            ImageLoader il = new ImageLoader(FriendListFragment.this.getActivity()
                    ,//ToDo: make a VkUserObjectImageSource
                    new DatabaseUserImageSource() {
                @Override
                protected int getUserId() {
                    return mCurrentUser.id;
                }

                @Override
                protected String getPhotoFieldName() {
                    return FriendsData.FIELDS_PHOTO200;
                }
            });
            il.setOnDownloadStartedListener(new ImageLoader.OnDownloadStartedListener() {
                @Override
                public void onDownloadStarted() {
                    mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mPhotoWaiter.setVisibility(View.VISIBLE);
                            }
                        }
                    );
                }
            });
            return  il;
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String photoFileName) {
            if (mAvatar != null) {
                if (photoFileName != null){
                    mAvatar.setImageURI(Uri.fromFile(new File(photoFileName)));
                } else {
                    mAvatar.setImageResource(R.drawable.blue_user_icon);
                }
            }
            mPhotoWaiter.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FriendListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FriendListFragment newInstance(VKApiUser user) {
        FriendListFragment fragment = new FriendListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ROOT_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mCurrentUser = getArguments().getParcelable(ARG_ROOT_USER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        mPhotoWaiter = (ProgressBar) view.findViewById(R.id.user_photo_waiter);
        mAvatar = (ImageView) view.findViewById(R.id.user_image);

        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            if (getActivity() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.setSupportActionBar(mToolbar);

                ActionBar bar = activity.getSupportActionBar();
                if (bar != null) {
                    bar.setDisplayHomeAsUpEnabled(true);
                }
            }
        }
        refreshTitle();
        //ToDo: start friend list loader

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        // Set the adapter
        if (recyclerView != null) {
            Context context = view.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new FriendsRecyclerViewAdapter(DummyContent.ITEMS, mListener));
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(DummyItem item);
    }

    private void refreshTitle() {
        mToolbar.setSubtitle(mCurrentUser.fields.optString(FriendsData.FIELDS_NAME_GEN));

        //ToDo: extract to a common Delegate
        final LoaderManager lm = getActivity().getSupportLoaderManager();
        final Loader ldr = lm.getLoader(FriendsData.LOADER_ID_WHOSE_PHOTO);

        //regardless whether we have the loader or not - reinitialize callbacks
        lm.initLoader(FriendsData.LOADER_ID_WHOSE_PHOTO, null, mUserPhotoLoaderCallback);

        //but restart loading if we did NOT have it before only
        if (ldr != null){
            ldr.onContentChanged();
        }
    }
}
