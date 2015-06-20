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

package com.appsimobile.appsii.module.people;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.appsimobile.appsii.PopupMenuHelper;
import com.appsimobile.appsii.R;
import com.appsimobile.appsii.module.AbstractContactView;
import com.appsimobile.appsii.module.BaseContactInfo;

/**
 * Created by nick on 03/06/14.
 */
public class ContactView extends AbstractContactView
        implements View.OnClickListener, LetterDecoratable, PopupMenu.OnMenuItemClickListener {

    View mOverflow;

    TextView mFirstLetterView;

    boolean mIsFirstLetterOfKind;

    String mLetter;

    PeopleActionListener mPeopleActionListener;

    public ContactView(Context context) {
        super(context);
    }

    public ContactView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPeopleActionListener(PeopleActionListener peopleActionListener) {
        mPeopleActionListener = peopleActionListener;
        if (peopleActionListener == null) {
            mOverflow.setVisibility(GONE);
        } else {
            mOverflow.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mOverflow = findViewById(R.id.overflow);
        mOverflow.setOnClickListener(this);
        mFirstLetterView = (TextView) findViewById(R.id.first_letter);
    }

    @Override
    public void onClick(View v) {
        PopupMenuHelper.showPopupMenu(v, R.menu.popup_people_item, this);
    }

    public void setFirstLetter(String letter) {
        mLetter = letter;
//        mFirstLetterView.setText(letter);
    }

    @Override
    public String getLetter() {
        return mLetter;
    }

    @Override
    public boolean isFirstLetterOfKind() {
        return mIsFirstLetterOfKind;
    }

    public void setFirstLetterOfKind(boolean isFirstLetterOfKind) {
        mIsFirstLetterOfKind = isFirstLetterOfKind;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            onEditClicked();
            return true;
        }
        return false;
    }

    private void onEditClicked() {
        if (mPeopleActionListener != null) {
            mPeopleActionListener.onEditSelected(mContactInfo);
        }
    }

    public interface PeopleActionListener {

        void onEditSelected(BaseContactInfo contactInfo);
    }
}
