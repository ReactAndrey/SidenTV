/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tvleanback.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.android.tvleanback.R;
import com.example.android.tvleanback.data.FetchVideoService;
import com.example.android.tvleanback.data.VideoContract;
import com.example.android.tvleanback.model.Video;
import com.example.android.tvleanback.model.VideoCursorMapper;
import com.example.android.tvleanback.presenter.CardPresenter;
import com.example.android.tvleanback.presenter.GridItemPresenter;
import com.example.android.tvleanback.presenter.IconHeaderItemPresenter;
import com.example.android.tvleanback.recommendation.UpdateRecommendationsService;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mCategoryRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private static final int CATEGORY_LOADER = 123; // Unique ID for Category Loader.
    private static final String SPECIAL_CATEGORY = "TV Shows";
    private int SUBCATEGORY_LOADER = 124; //Unique ID for SubCategory Loader
    // Sub Category
    private int special;
    ListRow subrow;
    private int addflag;
    private String subitempId;
    private static SQLiteDatabase sqliteDB = null;


    // Maps a Loader Id to its CursorObjectAdapter.
    private Map<Integer, CursorObjectAdapter> mVideoCursorAdapters;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
        getLoaderManager().initLoader(CATEGORY_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();
        setupEventListeners();
        prepareEntranceTransition();

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.
        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);

        updateRecommendations();

        // For Sub Category
        //createSubCategory();
        addflag = 0;
        special = -1;
        subitempId = "-1";

    }
    private void createSubCategory(){
        HeaderItem gridHeader = new HeaderItem("");//getString(R.string.more_samples));

        GridItemPresenter gridPresenter = new GridItemPresenter(this);
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        gridRowAdapter.add("No SubCategory");

        CursorObjectAdapter videoCursorAdapter =
                new CursorObjectAdapter(new CardPresenter());

        //subrow = new ListRow(gridHeader, gridRowAdapter);

        videoCursorAdapter.setMapper(new VideoCursorMapper());
        String dbName = "leanback.db";
        sqliteDB = getActivity().openOrCreateDatabase(dbName, MODE_PRIVATE, null);
        Cursor cursor = null;
        String sql = "SELECT * FROM video where category='" + SPECIAL_CATEGORY + "' and " + "pid='" + subitempId + "'" + " and pid!='0'";
        cursor = sqliteDB.rawQuery(sql,null);

        videoCursorAdapter.changeCursor(cursor);
        if(cursor!=null) {
            subrow = new ListRow(gridHeader, videoCursorAdapter);
        }
        else
            subrow = new ListRow(gridHeader, gridRowAdapter);
    }
    @Override
    public void onDestroy() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
            mBackgroundTimer = null;
        }
        mBackgroundManager = null;
        super.onDestroy();
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
//        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.videos_by_google_banner, null));
//        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        //setHeadersState(HEADERS_HIDDEN);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);
        getView().setBackground(mDefaultBackground);
       // this.setTitle("aaa");
                //.getHeadersSupportFragment
//        Glide.with(this)
//                .asBitmap()
//                .load(uri)
//                .apply(options)
//                .into(new SimpleTarget<Bitmap>(width, height) {
//                    @Override
//                    public void onResourceReady(
//                            Bitmap resource,
//                            Transition<? super Bitmap> transition) {
//                        mBackgroundManager.setBitmap(resource);
//                    }
//                });

                if(special==1 && addflag==0) {
                    createSubCategory();
                    if(subrow!=null) {
                        mCategoryRowAdapter.add(3, subrow);
                        addflag = 1;
                    }
                }
                else if(special==1 && addflag==1){
                    if(subrow!=null)
                        mCategoryRowAdapter.remove(subrow);
                    //mCategoryRowAdapter.removeItems(1,1);
                    createSubCategory();
                    if(subrow!=null)
                    mCategoryRowAdapter.add(3, subrow);
                }
                else if(special==0 && addflag==1){
                    if(subrow!=null) {
                        mCategoryRowAdapter.remove(subrow);
                        //mCategoryRowAdapter.removeItems(1,1);
                        addflag = 0;
                    }

                }


                    //mCategoryRowAdapter.removeItems(1,1);

                //mCategoryRowAdapter.add(1,row);

                //mCategoryRowAdapter.add(row);


        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private void updateRecommendations() {
        Intent recommendationIntent = new Intent(getActivity(), UpdateRecommendationsService.class);
        getActivity().startService(recommendationIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == CATEGORY_LOADER) {
            //String str = "content://com.example.android.tvleanback/video";
            Uri t = Uri.parse("content://com.example.android.tvleanback" ).buildUpon().appendPath("video").build();
            return new CursorLoader(
                    getContext(),
                    t,//VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract.VideoEntry.COLUMN_CATEGORY},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );
        }
        else {
            // Assume it is for a video.
            String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);
            String cstr = "0";
            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?" + " and " + VideoContract.VideoEntry.COLUMN_PID + " = ?", // Selection clause
                    new String[]{category,cstr},  // Select based on the category id.
                    null // Default sort order
            );
        }
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int i = 0;
        if (data != null && data.moveToFirst()  ) {
            final int loaderId = loader.getId();

            if (loaderId == CATEGORY_LOADER) {

                // Every time we have to re-get the category loader, we must re-create the sidebar.
                mCategoryRowAdapter.clear();

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex =
                            data.getColumnIndex(VideoContract.VideoEntry.COLUMN_CATEGORY);
                  //  int pidIndex =
                          //  data.getColumnIndex(VideoContract.VideoEntry.COLUMN_PID);
                    String category = data.getString(categoryIndex);

                    //Log.i("CategoryStrin=",String.valueOf(pidIndex));
                    // Create header for this category.
                    HeaderItem header = new HeaderItem(category);//"Recently Showed");

                    int videoLoaderId = category.hashCode(); // Create unique int from category.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter =
                                new CursorObjectAdapter(new CardPresenter());
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        //if(pid.contains("0"))

                        if(category.contains(SPECIAL_CATEGORY)){
                            mVideoCursorAdapters.put(SUBCATEGORY_LOADER,videoCursorAdapter);
                        }

                        mCategoryRowAdapter.add(row);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, category);
                        getLoaderManager().initLoader(videoLoaderId, args, this);
                    } else {
                        ListRow row = new ListRow(header, existingAdapter);
                        mCategoryRowAdapter.add(row);
                    }

                    data.moveToNext();
                }

                // Create a row for this special case with more samples.
//                HeaderItem gridHeader = new HeaderItem("");//getString(R.string.more_samples));
//                GridItemPresenter gridPresenter = new GridItemPresenter(this);
//                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
//                gridRowAdapter.add(getString(R.string.grid_view));
//                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
//                gridRowAdapter.add(getString(R.string.error_fragment));
//                gridRowAdapter.add(getString(R.string.personal_settings));
//                ListRow row = new ListRow(gridHeader, gridRowAdapter);
//                mCategoryRowAdapter.add(row);
                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.
            } else {
                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);
            }
        } else {
            // Start an Intent to fetch the videos.
            Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
            getActivity().startService(serviceIntent);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int loaderId = loader.getId();
        if (loaderId != CATEGORY_LOADER) {
            mVideoCursorAdapters.get(loaderId).changeCursor(null);
        } else {
            mCategoryRowAdapter.clear();
        }
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI.toString());
                    }
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            Log.i("Error=","error");
            if (item instanceof Video) {
                Video video = (Video) item;

                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();

//                subitempId = "-1";
//                if(((Video)item).category.contains(SPECIAL_CATEGORY)) {
//                    special = 1;
//                    subitempId = ((Video)item).pid;
//                    startBackgroundTimer();
//                    return;
//                }else
//                    special = 0;
                //Toast.makeText(getActivity(), "select" + ((Video)item).pid, Toast.LENGTH_SHORT).show();
                //Log.i("select","sel");
                if(((Video)item).category.contains(SPECIAL_CATEGORY) && (Integer.parseInt(((Video)item).pid))!=0) {
                    getActivity().startActivity(intent, bundle);
                } else if(!((Video)item).category.contains(SPECIAL_CATEGORY) && (Integer.parseInt(((Video)item).pid))==0) {
                    getActivity().startActivity(intent, bundle);
                }

            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.grid_view))) {
                    Intent intent = new Intent(getActivity(), VerticalGridActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
                    Intent intent = new Intent(getActivity(), GuidedStepActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.error_fragment))) {
                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment)
                            .addToBackStack(null).commit();
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                special = 0;
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);

                subitempId = "-1";
                if(((Video)item).category.contains(SPECIAL_CATEGORY)) {
                    special = 1;
                    subitempId = ((Video)item).mid;
                }
                if(((Video)item).category.contains(SPECIAL_CATEGORY) && (Integer.parseInt(((Video)item).pid))!=0) {
                    //Toast.makeText(getActivity(), "select" + ((Video)item).category, Toast.LENGTH_SHORT).show();
                    special = -1;
                }
                startBackgroundTimer();
            }

        }
    }
}
