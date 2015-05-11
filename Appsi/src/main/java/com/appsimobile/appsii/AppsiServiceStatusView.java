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
 */

package com.appsimobile.appsii;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A simple view that displays the running status of Appsii.
 * <p/>
 * Created by Nick Martens on 9/9/13.
 */
public class AppsiServiceStatusView extends RelativeLayout implements View.OnClickListener {

    AnalyticsManager mAnalyticsManager = AnalyticsManager.getInstance();

    TextView mStatusView;

    ImageView mActionButton;

    boolean mIgnoreOrderedBroadcastResult;

    int mStatus;

    private BroadcastReceiver mResultReceiver;

    private BroadcastReceiver mAppsiStartedReceiver;

    public AppsiServiceStatusView(Context context) {
        super(context);
    }

    public AppsiServiceStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public AppsiServiceStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!mIgnoreOrderedBroadcastResult) {
                    int resultCode = getResultCode();
                    updateStatus(resultCode);
                }
            }
        };
        mAppsiStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AppsiiUtils.ACTION_STARTED.equals(intent.getAction())) {
                    onAppsiStarted();
                    mIgnoreOrderedBroadcastResult = true;
                } else if (AppsiiUtils.ACTION_APPSI_STATUS_CHANGED.equals(
                        intent.getAction())) {
                    onAppsiStatusChanged();
                }
            }
        };

        updateAppsiStatus();

        mStatusView = (TextView) findViewById(R.id.status_view);
        mActionButton = (ImageView) findViewById(R.id.action_button);

        mActionButton.setOnClickListener(this);
    }

    void updateStatus(int resultCode) {
        switch (resultCode) {
            case Appsi.RESULT_RUNNING_STATUS_ENABLED:
                onAppsiEnabled();
                break;
            case Appsi.RESULT_RUNNING_STATUS_SUSPENDED:
            case Appsi.RESULT_RUNNING_STATUS_DISABLED:
            default:
                onAppsiDisabled();
                break;
        }
    }

    void onAppsiStarted() {
        onAppsiEnabled();
    }

    void onAppsiStatusChanged() {
        updateAppsiStatus();
    }

    private void updateAppsiStatus() {
        mIgnoreOrderedBroadcastResult = false;

        Intent i = new Intent(Appsi.ACTION_ORDERED_BROADCAST_RUNNING);
        i.setPackage(getContext().getPackageName());
        getContext().sendOrderedBroadcast(i, null, mResultReceiver, null,
                Appsi.RESULT_RUNNING_STATUS_DISABLED, null, null);
    }

    private void onAppsiEnabled() {
        mStatusView.setText(R.string.appsi_status_running);
        mActionButton.setImageResource(R.drawable.ic_media_stop);
        mStatus = Appsi.RESULT_RUNNING_STATUS_ENABLED;
    }

    private void onAppsiDisabled() {
        mStatusView.setText(R.string.appsi_status_stopped);
        mActionButton.setImageResource(R.drawable.ic_media_play);
        mStatus = Appsi.RESULT_RUNNING_STATUS_DISABLED;
    }

    @Override
    public void onClick(View v) {
        switch (mStatus) {
            case Appsi.RESULT_RUNNING_STATUS_DISABLED:
                Intent i = new Intent(getContext(), Appsi.class);
                getContext().startService(i);
                track(AnalyticsManager.ACTION_START_APPSI, AnalyticsManager.CATEGORY_OTHER);
                break;
            case Appsi.RESULT_RUNNING_STATUS_SUSPENDED:
                Intent unSuspend = new Intent(AppsiiUtils.ACTION_UNSUSPEND);
                getContext().sendBroadcast(unSuspend);
                track(AnalyticsManager.ACTION_START_APPSI, AnalyticsManager.CATEGORY_OTHER);
                onAppsiEnabled();
                break;
            case Appsi.RESULT_RUNNING_STATUS_ENABLED:
                Intent stop = new Intent(Appsi.ACTION_STOP_APPSI);
                getContext().sendBroadcast(stop);
                onAppsiDisabled();
                track(AnalyticsManager.ACTION_STOP_APPSI, AnalyticsManager.CATEGORY_OTHER);
                break;
        }
    }

    public void track(String action, String category) {
        mAnalyticsManager.trackAppsiEvent(action, category);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter statusReceiverFilter = new IntentFilter(AppsiiUtils.ACTION_STARTED);
        statusReceiverFilter.addAction(AppsiiUtils.ACTION_APPSI_STATUS_CHANGED);
        getContext().registerReceiver(mAppsiStartedReceiver, statusReceiverFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mAppsiStartedReceiver);
    }

    public void track(String action, String category, String label) {
        mAnalyticsManager.trackAppsiEvent(action, category, label);
    }


}
