/**
 * Hubroid - A GitHub app for Android
 *
 * Copyright (c) 2011 Eddie Ringle.
 *
 * Licensed under the New BSD License.
 */

package net.idlesoft.android.apps.github.activities;

import net.idlesoft.android.apps.github.R;
import net.idlesoft.android.apps.github.utils.GravatarCache;

import org.idlesoft.libraries.ghapi.APIAbstract.Response;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SingleCommit extends BaseActivity {
    private static class GetCommitTask extends AsyncTask<Void, Void, Void> {
        public SingleCommit activity;

        @Override
        protected Void doInBackground(final Void... params) {
            try {
                // TODO: Convert to use egit-github
                final Response commitResponse = activity.mGApi.commits.commit(
                        activity.mRepositoryOwner, activity.mRepositoryName, activity.mCommitSha);
                if (commitResponse.statusCode != 200) {
                    /*
                     * Oh noez, something went wrong... TODO: Do some failure
                     * handling here
                     */
                    return null;
                }
                activity.mJson = (new JSONObject(commitResponse.resp)).getJSONObject("commit");

                // This Activity has two entry points:
                // * From CommitsList which provides an author and committer in
                // the bundle.
                // * From SingleActivityItem which does not.
                // If the author or committer are null, populate them from the
                // JSON data.
                if (activity.mAuthor == null) {
                    activity.mAuthor = activity.mJson.getJSONObject("author").getString("login");
                }

                if (activity.mCommitter == null) {
                    activity.mCommitter = activity.mJson.getJSONObject("committer").getString(
                            "login");
                }

                activity.mAuthorName = activity.mJson.getJSONObject("author").getString("name");
                activity.mCommitterName = activity.mJson.getJSONObject("committer").getString(
                        "name");

                activity.mAuthorGravatar = SingleCommit.loadGravatarByLoginName(activity,
                        activity.mAuthor);
                activity.mCommitterGravatar = SingleCommit.loadGravatarByLoginName(activity,
                        activity.mCommitter);
            } catch (final JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            if (activity.mJson != null) {
                activity.buildUi();
            }
            activity.mProgressLayout.setVisibility(View.GONE);
            activity.mCommitLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
            activity.mCommitLayout.setVisibility(View.GONE);
            activity.mProgressLayout.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }
    }

    public static String getHumanDate(final Date current_time, final Date commit_time) {
        String end;
        final long ms = current_time.getTime() - commit_time.getTime();
        final long sec = ms / 1000;
        final long min = sec / 60;
        final long hour = min / 60;
        final long day = hour / 24;
        if (day > 0) {
            if (day == 1) {
                end = " day ago";
            } else {
                end = " days ago";
            }
            return day + end;
        } else if (hour > 0) {
            if (hour == 1) {
                end = " hour ago";
            } else {
                end = " hours ago";
            }
            return hour + end;
        } else if (min > 0) {
            if (min == 1) {
                end = " minute ago";
            } else {
                end = " minutes ago";
            }
            return min + end;
        } else {
            if (sec == 1) {
                end = " second ago";
            } else {
                end = " seconds ago";
            }
            return sec + end;
        }
    }

    /**
     * Get the Gravatars of all users in the commit log
     */
    public static Bitmap loadGravatarByLoginName(final Activity pActivity, final String pLogin) {
        if (pLogin != null && !pLogin.equals("")) {
            return GravatarCache.getDipGravatar(pLogin, 30.0f, pActivity.getResources()
                    .getDisplayMetrics().density);
        } else {
            return null;
        }
    }

    private String mAuthor;

    private Bitmap mAuthorGravatar;

    private String mAuthorName;

    private ScrollView mCommitLayout;

    private String mCommitSha;

    private String mCommitter;

    private Bitmap mCommitterGravatar;

    private String mCommitterName;

    private GetCommitTask mGetCommitTask;

    public Intent mIntent;

    private JSONObject mJson;

    private RelativeLayout mProgressLayout;

    private String mRepositoryName;

    private String mRepositoryOwner;

    protected void buildUi() {
        // Get the commit data for that commit ID so that we can get the
        // tree ID and filename.
        try {
            final ImageView authorImage = (ImageView) findViewById(R.id.commit_view_author_gravatar);
            final ImageView committerImage = (ImageView) findViewById(R.id.commit_view_committer_gravatar);

            // If the committer is the author then just show them as the
            // author, otherwise show
            // both people
            ((TextView) findViewById(R.id.commit_view_author_name)).setText(mAuthorName);
            if (mAuthorGravatar != null) {
                authorImage.setImageBitmap(mAuthorGravatar);
            } else {
                authorImage.setImageBitmap(SingleCommit.loadGravatarByLoginName(SingleCommit.this,
                        mAuthor));
            }

            // Set the commit message
            ((TextView) findViewById(R.id.commit_view_message)).setText(mJson.getString("message"));

            final SimpleDateFormat dateFormat = new SimpleDateFormat(Hubroid.GITHUB_TIME_FORMAT);
            Date commit_time;
            Date current_time;
            String authorDate = "";

            try {
                commit_time = dateFormat.parse(mJson.getString("authored_date"));
                current_time = new Date();
                ((TextView) findViewById(R.id.commit_view_author_time)).setText(SingleCommit
                        .getHumanDate(current_time, commit_time));

                commit_time = dateFormat.parse(mJson.getString("committed_date"));
                authorDate = SingleCommit.getHumanDate(current_time, commit_time);

            } catch (final ParseException e) {
                e.printStackTrace();
            }

            if (!mAuthor.equals(mCommitter)) {
                // They are not the same person, make the author visible and
                // fill in the details
                ((LinearLayout) findViewById(R.id.commit_view_author_layout))
                        .setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.commit_view_committer_name)).setText(mCommitterName);
                ((TextView) findViewById(R.id.commit_view_committer_time)).setText(authorDate);
                if (mCommitterGravatar != null) {
                    committerImage.setImageBitmap(mCommitterGravatar);
                } else {
                    committerImage.setImageBitmap(SingleCommit.loadGravatarByLoginName(
                            SingleCommit.this, mCommitter));
                }
            }

            final OnClickListener onGravatarClick = new OnClickListener() {
                public void onClick(final View v) {
                    final Intent i = new Intent(SingleCommit.this, Profile.class);
                    if (v.getId() == authorImage.getId()) {
                        i.putExtra("username", mAuthor);
                    } else if (v.getId() == committerImage.getId()) {
                        i.putExtra("username", mCommitter);
                    } else {
                        return;
                    }
                    startActivity(i);
                }
            };

            if ((mAuthor != null) && !mAuthor.equals("")) {
                authorImage.setOnClickListener(onGravatarClick);
            }
            if ((mCommitter != null) && !mCommitter.equals("")) {
                committerImage.setOnClickListener(onGravatarClick);
            }

            int filesAdded, filesRemoved, filesChanged;

            try {
                filesAdded = mJson.getJSONArray("added").length();
            } catch (final JSONException e) {
                filesAdded = 0;
            }
            try {
                filesRemoved = mJson.getJSONArray("removed").length();
            } catch (final JSONException e) {
                filesRemoved = 0;
            }
            try {
                filesChanged = mJson.getJSONArray("modified").length();
            } catch (final JSONException e) {
                filesChanged = 0;
            }

            final Button filesAddedButton = (Button) findViewById(R.id.btn_commit_addedFiles);
            final Button filesRemovedButton = (Button) findViewById(R.id.btn_commit_removedFiles);
            final Button filesChangedButton = (Button) findViewById(R.id.btn_commit_changedFiles);

            Log.d("debug", filesAdded + " " + filesRemoved + " " + filesChanged);
            if (filesAdded > 0) {
                filesAddedButton.setText(filesAdded + " files added");
            } else {
                filesAddedButton.setVisibility(View.GONE);
            }
            if (filesRemoved > 0) {
                filesRemovedButton.setText(filesRemoved + " files removed");
            } else {
                filesRemovedButton.setVisibility(View.GONE);
            }
            if (filesChanged > 0) {
                filesChangedButton.setText(filesChanged + " files changed");
            } else {
                filesChangedButton.setVisibility(View.GONE);
            }

            filesAddedButton.setOnClickListener(new OnClickListener() {
                public void onClick(final View v) {
                    final Intent i = new Intent(SingleCommit.this, DiffFilesList.class);
                    i.putExtra("type", "added");
                    i.putExtra("json", mJson.toString());
                    i.putExtra("repo_owner", mRepositoryOwner);
                    i.putExtra("repo_name", mRepositoryName);
                    startActivity(i);
                }
            });
            filesRemovedButton.setOnClickListener(new OnClickListener() {
                public void onClick(final View v) {
                    final Intent i = new Intent(SingleCommit.this, DiffFilesList.class);
                    i.putExtra("type", "removed");
                    i.putExtra("json", mJson.toString());
                    i.putExtra("repo_owner", mRepositoryOwner);
                    i.putExtra("repo_name", mRepositoryName);
                    startActivity(i);
                }
            });
            filesChangedButton.setOnClickListener(new OnClickListener() {
                public void onClick(final View v) {
                    final Intent i = new Intent(SingleCommit.this, DiffFilesList.class);
                    i.putExtra("type", "modified");
                    i.putExtra("json", mJson.toString());
                    i.putExtra("repo_owner", mRepositoryOwner);
                    i.putExtra("repo_name", mRepositoryName);
                    startActivity(i);
                }
            });
        } catch (final JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle, R.layout.commit);

        setupActionBar();

        mCommitLayout = (ScrollView) findViewById(R.id.sv_commit_content);
        mProgressLayout = (RelativeLayout) findViewById(R.id.rl_commit_progressLayout);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mRepositoryName = extras.getString("repo_name");
            mRepositoryOwner = extras.getString("repo_owner");
            mCommitSha = extras.getString("commit_sha");
            mCommitter = extras.getString("committer");
            mAuthor = extras.getString("author");
        }
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        try {
            if (savedInstanceState.containsKey("json")) {
                mJson = new JSONObject(savedInstanceState.getString("json"));
            }
            if (mJson != null) {
                buildUi();
            }
        } catch (final JSONException e) {
            e.printStackTrace();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        mGetCommitTask = (GetCommitTask) getLastNonConfigurationInstance();
        if (mGetCommitTask == null) {
            mGetCommitTask = new GetCommitTask();
        }
        mGetCommitTask.activity = SingleCommit.this;
        if ((mGetCommitTask.getStatus() == AsyncTask.Status.PENDING) && (mJson == null)) {
            mGetCommitTask.execute();
        } else {
            mCommitLayout.setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mGetCommitTask;
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        if (mJson != null) {
            outState.putString("json", mJson.toString());
        }
        super.onSaveInstanceState(outState);
    }
}
