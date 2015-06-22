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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appsimobile.appsii.annotation.VisibleForTesting;
import com.appsimobile.appsii.module.home.provider.HomeContract;

import java.util.List;

/**
 * A pager adapter that holds client state. This is modeled after
 * fragments and the FragmentStatePagerAdapter
 * Created by nick on 10/08/14.
 */
public abstract class AbstractSidebarPagerAdapter extends PagerAdapter {

    /**
     * A controller frag that indicates that the background and
     * other decorations should not be shown
     */
    public static final int FLAG_NO_DECORATIONS = 1;

    static final int CLOSE_ACTION_AUTO_CLOSE = PageController.CLOSE_ACTION_AUTO_CLOSE;

    static final int CLOSE_ACTION_KEEP_OPEN = PageController.CLOSE_ACTION_KEEP_OPEN;

    static final int CLOSE_ACTION_ASK = PageController.CLOSE_ACTION_ASK;

    static final int CLOSE_ACTION_DONT_KNOW = PageController.CLOSE_ACTION_DONT_KNOW;

    @VisibleForTesting
    final SimpleArrayMap<HotspotPageEntry, PageController> mCachedControllers =
            new SimpleArrayMap<>();

    @VisibleForTesting
    final SparseArray<HotspotPageEntry> mActivePageKeys = new SparseArray<>(8);

    @VisibleForTesting
    final SparseArray<PageController> mActivePageControllers = new SparseArray<>(8);

    @VisibleForTesting
    final SparseArray<PageController> mPrevPageControllers = new SparseArray<>(8);

    final boolean mDontCachePages = true;

    private final SimpleArrayMap<HotspotPageEntry, Bundle> mSavedControllerStates =
            new SimpleArrayMap<>();

    private final Context mContext;

    FlagListener mFlagListener;

    boolean mAttachedToWindow;

    @VisibleForTesting
    PageController mActivePageController;

    public AbstractSidebarPagerAdapter(
            Context context) {
        mContext = context;
    }

    protected String getString(@StringRes int stringResId) {
        return mContext.getString(stringResId);
    }

    public void setFlagListener(FlagListener flagListener) {
        mFlagListener = flagListener;
    }

    public void setPages(List<HotspotPageEntry> pages) {
        if (mActivePageKeys.equals(pages)) return;

        // temporary keep the old items in a separate list so we can
        // figure out the exact change in getItemPosition
        mPrevPageControllers.clear();

        int count = mActivePageControllers.size();
        for (int i = 0; i < count; i++) {
            int key = mActivePageControllers.keyAt(i);
            PageController controller = mActivePageControllers.valueAt(i);
            mPrevPageControllers.put(key, controller);
        }

        mActivePageKeys.clear();
        mActivePageControllers.clear();

        int position = 0;
        for (HotspotPageEntry page : pages) {
            if (!page.mEnabled) continue;
            if (page.mPageType == HomeContract.Pages.PAGE_SETTINGS) continue;
            if (page.mPageType == HomeContract.Pages.PAGE_SMS) continue;

            PageController controller = mCachedControllers.get(page);

            if (controller == null) {
                controller = instantiateAndCacheControllerForPage(page);
            }
            mActivePageKeys.put(position, page);
            mActivePageControllers.put(position, controller);

            position++;
        }
        notifyDataSetChanged();
    }

    private PageController instantiateAndCacheControllerForPage(HotspotPageEntry page) {
        Bundle state = mSavedControllerStates.get(page);
        PageController controller = createPageController(page, state);
        mCachedControllers.put(page, controller);
        return controller;
    }

    public final PageController createPageController(HotspotPageEntry page, Bundle state) {
        return onCreatePageController(mContext, page);
    }

    protected abstract PageController onCreatePageController(Context context,
            HotspotPageEntry page);

    HotspotPageEntry getPageKey(int position) {
        return mActivePageKeys.get(position);
    }

    @Override
    public int getCount() {
        return mActivePageKeys.size();
    }

    @Override
    public final Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        PageController controller = mActivePageControllers.get(position);

        if (controller == null) {
            HotspotPageEntry page = mActivePageKeys.get(position);
            controller = instantiateAndCacheControllerForPage(page);
        }


        HotspotPageEntry page = mActivePageKeys.get(position);
        controller.setPage(page);

        Bundle state = mSavedControllerStates.get(page);
        controller.performCreate(state);

        View result = controller.performCreateView(inflater, container);
        controller.performViewCreated(result);

        if (mAttachedToWindow) {
            controller.onSidebarAttached();
        } else {
            controller.onSidebarDetached();
        }


        container.addView(controller.getView());
        controller.performOnAttach();
        controller.performStart();
        controller.performRestoreInstanceState();
        controller.performResume();

        return controller;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        PageController controller = (PageController) object;

        HotspotPageEntry page = controller.getPage();

        Bundle savedControllerState = mSavedControllerStates.get(page);
        if (savedControllerState == null) {
            savedControllerState = new Bundle();
            mSavedControllerStates.put(page, savedControllerState);
        } else {
            savedControllerState.clear();
        }
        controller.performPause();
        controller.performSaveInstanceState(savedControllerState);
        controller.performStop();
        controller.performOnDetach();
        container.removeView(controller.getView());
        controller.performDestroy();

        if (mDontCachePages) {
            mActivePageControllers.remove(position);
            HotspotPageEntry e = controller.getPage();
            mCachedControllers.remove(e);
        }

    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
        if (mActivePageController != object) {

            if (mActivePageController != null) {
                mActivePageController.setUserVisible(false);
                mActivePageController.onUserInvisible();
            }

            mActivePageController = (PageController) object;

            // TODO: probably happens when the user disabled all pages (?)
            if (mActivePageController != null) {
                mActivePageController.setUserVisible(true);
                mActivePageController.onUserVisible();
                updatePageFlags(mActivePageController.getFlags());
            } else {
                updatePageFlags(0);
            }
        }
    }

    protected void updatePageFlags(int i) {
        if (mFlagListener != null) {
            mFlagListener.onFlagsChanged(i);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        PageController controller = (PageController) object;
        return view == controller.getView();
    }

    @Override
    public int getItemPosition(Object object) {
        int oldPos = keyOf(mPrevPageControllers, (PageController) object);
        int newIdx = keyOf(mActivePageControllers, (PageController) object);
        if (oldPos != -1) {
            mPrevPageControllers.remove(oldPos);
        }

        if (newIdx == -1) {
            return POSITION_NONE;
        } else if (newIdx == oldPos) {
            return POSITION_UNCHANGED;
        } else {
            return newIdx;
        }
    }

    <T> int keyOf(SparseArray<T> array, T object) {
        int length = array.size();
        for (int i = 0; i < length; i++) {
            T value = array.valueAt(i);
            if (value == object) return array.keyAt(i);
        }
        return -1;
    }

    public void onTrimMemory(int level) {
        int length = mActivePageControllers.size();
        for (int i = 0; i < length; i++) {
            PageController controller = mActivePageControllers.get(i);
            if (controller != null) {
                controller.onTrimMemory(level);
            }
        }
    }

    public void setSidebarClosed() {
        if (mActivePageController != null) {
            mActivePageController.onUserInvisible();
        }

    }

    public void setSidebarOpening(boolean sidebarOpening) {
        int length = mActivePageControllers.size();
        for (int i = 0; i < length; i++) {
            PageController controller = mActivePageControllers.get(i);
            if (controller != null) {
                controller.setDeferLoads(sidebarOpening);
            }
        }
        if (mActivePageController != null) {
            if (sidebarOpening) {
                mActivePageController.onUserInvisible();
            } else {
                mActivePageController.onUserVisible();
            }
            // TODO: how come this is not properly called when after using try; loading home?
            // seems like the new active page has not yet been added to the list of
            // ActivePageControllers
            mActivePageController.setDeferLoads(sidebarOpening);
        }
    }

    public void onAttachedToWindow() {
        mAttachedToWindow = true;
        int length = mActivePageControllers.size();
        for (int i = 0; i < length; i++) {
            PageController controller = mActivePageControllers.get(i);
            if (controller != null) {
                controller.onSidebarAttached();
            }
        }

    }

    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        int length = mActivePageControllers.size();
        for (int i = 0; i < length; i++) {
            PageController controller = mActivePageControllers.get(i);
            if (controller != null) {
                controller.onSidebarDetached();
            }
        }

    }

    public int shouldClose(Bundle state) {
        if (mActivePageController != null) {
            return mActivePageController.shouldClose(state);
        }
        return PageController.CLOSE_ACTION_DONT_KNOW;
    }

    public void rememberCloseAction(Bundle state, int action) {
        if (mActivePageController != null) {
            mActivePageController.rememberCloseAction(state, action);
        }
    }

    interface FlagListener {

        void onFlagsChanged(int flags);
    }


}
