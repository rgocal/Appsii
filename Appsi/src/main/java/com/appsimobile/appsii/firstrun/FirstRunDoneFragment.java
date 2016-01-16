/*
 * Copyright 2015. Appsi Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.appsimobile.appsii.firstrun;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.appsimobile.appsii.AccountHelper;
import com.appsimobile.appsii.AnalyticsManager;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.dagger.AppInjector;

import javax.inject.Inject;

/**
 * Created by nick on 10/06/15.
 */
public final class FirstRunDoneFragment extends Fragment implements View.OnClickListener {

    Button mNextButton;
    Button mJoinButton;
    @Inject
    AnalyticsManager mAnalyticsManager;
    @Inject
    AccountHelper mAccountHelper;
    private OnDoneCompletedListener mOnDoneCompletedListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first_run_done, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInjector.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNextButton = (Button) view.findViewById(R.id.next_button);
        mJoinButton = (Button) view.findViewById(R.id.join_button);

        mNextButton.setOnClickListener(this);
        mJoinButton.setOnClickListener(this);
    }

    public void setDoneCompletedListener(OnDoneCompletedListener onDoneCompletedListener) {
        mOnDoneCompletedListener = onDoneCompletedListener;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.next_button:
                onNextButtonPressed();
                break;
            case R.id.join_button:
                onJoinPressed();
                break;
        }
    }

    private void onJoinPressed() {
        Uri uri = Uri.parse("https://plus.google.com/communities/111374377186674137148");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
        mAnalyticsManager.trackAppsiEvent(AnalyticsManager.ACTION_OPEN_GOOGLE_COMMUNITY,
                AnalyticsManager.CATEGORY_WELCOME);

    }

    private void onNextButtonPressed() {
        mAccountHelper.configureAutoSyncAndSync();
        mOnDoneCompletedListener.onDoneCompleted();
    }


    public interface OnDoneCompletedListener {

        void onDoneCompleted();
    }
}
