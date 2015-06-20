/*
 *
 *  * Copyright 2015. Appsi Mobile
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.appsimobile.appsii.module.home;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.test.InstrumentationTestCase;

import com.appsimobile.appsii.module.home.TestOpsBuilder.OperationWrapper;
import com.appsimobile.appsii.module.home.provider.HomeContract;
import com.appsimobile.appsii.module.home.provider.HomeContract.Cells;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nick on 11/03/15.
 */
public class HomeAdapterEditorTest extends InstrumentationTestCase {

    Context mContext;

    // TODO: test delete rows
    // TODO: test delete cell

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    @android.test.UiThreadTest
    public void testGetItemsInRow() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(1).build(), // 0
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build(), // 1
                buildHomeItem(3L).row(2L, 2).position(0).colspan(2).build(), // 2
                buildHomeItem(4L).row(2L, 2).position(1).colspan(2).build(), // 3
                buildHomeItem(5L).row(3L, 3).position(0).colspan(1).build(), // 4
                buildHomeItem(6L).row(3L, 3).position(1).colspan(1).build(), // 5
                buildHomeItem(11L).row(3L, 3).position(2).colspan(1).build(), // 6
                buildHomeItem(7L).row(4L, 4).position(0).colspan(3).build(), // 7
                buildHomeItem(9L).row(5L, 5).position(0).colspan(2).build(), // 8
                buildHomeItem(10L).row(5L, 5).position(1).colspan(1).build() // 9
        );

        List<HomeItem> list = new ArrayList<>(4);
        t.mHomeAdapter.getItemsInRow(2L, list);
        assertEquals(2, list.size());
        assertEquals(3L, list.get(0).mId);
        assertEquals(4L, list.get(1).mId);
        list.clear();

        t.mHomeAdapter.getItemsInRow(1L, list);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).mId);
        assertEquals(2L, list.get(1).mId);
        list.clear();

        t.mHomeAdapter.getItemsInRow(3L, list);
        assertEquals(3, list.size());
        assertEquals(5L, list.get(0).mId);
        assertEquals(6L, list.get(1).mId);
        assertEquals(11L, list.get(2).mId);
        list.clear();

        t.mHomeAdapter.getItemsInRow(4L, list);
        assertEquals(1, list.size());
        assertEquals(7L, list.get(0).mId);
        list.clear();

        t.mHomeAdapter.getItemsInRow(5L, list);
        assertEquals(2, list.size());
        assertEquals(9L, list.get(0).mId);
        assertEquals(10L, list.get(1).mId);
        list.clear();

        t.mHomeAdapter.getItemsInRow(6L, list);
        assertEquals(0, list.size());
        list.clear();

    }

    HomeItemBuilder buildHomeItem(long id) {
        return new HomeItemBuilder(id);
    }

    @android.test.UiThreadTest
    public void testRemoveCell_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(4L).row(1L, 1).position(0).height(2).build(), // 0
                buildHomeItem(5L).row(1L, 1).position(1).height(2).build(), // 1
                buildHomeItem(1L).row(2L, 2).position(0).build(), // 2
                buildHomeItem(7L).row(2L, 2).position(1).build(), // 3
                buildHomeItem(2L).row(2L, 2).position(2).colspan(2).build(), // 4
                buildHomeItem(12L).row(5L, 3).position(0).build(), // 5
                buildHomeItem(8L).row(3L, 4).position(0).build(), // 6
                buildHomeItem(9L).row(3L, 4).position(1).build() // 7
        );

        t.mEditor.removeCellAtPosition(-1, t.mOps);
        t.assertItemsNotChanged();
        assertEquals(0, t.opsSize());
        t.reset(false);

        t.mEditor.removeCellAtPosition(0, t.mOps);
        {
            t.assertItemsNotChanged();
            // op1: delete
            // op2: update 5L
            assertEquals(2, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);
            OperationWrapper op2 = t.wrapperAt(1);

            // check that this is a delete of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(0, getIntFrom(op1, Cells.POSITION));
            assertEquals(ContentUris.withAppendedId(Cells.CONTENT_URI, 5L), op1.mUri);

            // check that this is a delete of the cell
            assertEquals(OperationWrapper.TYPE_DELETE, op2.mType);
            assertEquals(ContentUris.withAppendedId(Cells.CONTENT_URI, 4L), op2.mUri);
            assertEquals(4L, op2.uriId());

            t.reset(false);
        }

        t.mEditor.removeCellAtPosition(1, t.mOps);
        {
            t.assertItemsNotChanged();
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is a delete of the cell
            assertEquals(OperationWrapper.TYPE_DELETE, op1.mType);
            assertEquals(5L, op1.uriId());

            t.reset(false);
        }


        t.mEditor.removeCellAtPosition(2, t.mOps);
        t.assertItemsNotChanged();
        // op1: delete
        // op2: update 5L
        assertEquals(3, t.opsSize());
        {
            OperationWrapper op1 = t.wrapperAt(0);
            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(0, getIntFrom(op1, Cells.POSITION));
            assertEquals(ContentUris.withAppendedId(Cells.CONTENT_URI, 7L), op1.mUri);
        }
        {
            OperationWrapper op2 = t.wrapperAt(1);
            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op2.mType);
            assertEquals(1, getIntFrom(op2, Cells.POSITION));
            assertEquals(ContentUris.withAppendedId(Cells.CONTENT_URI, 2L), op2.mUri);
        }
        {
            OperationWrapper op = t.wrapperAt(2);
            // check that this is a delete of the cell
            assertEquals(OperationWrapper.TYPE_DELETE, op.mType);
            assertEquals(ContentUris.withAppendedId(Cells.CONTENT_URI, 1L), op.mUri);
        }
        t.reset(false);

        t.mEditor.removeCellAtPosition(3, t.mOps);
        {
            t.assertItemsNotChanged();
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is a delete of the cell
            assertEquals(OperationWrapper.TYPE_DELETE, op1.mType);
            assertEquals(7L, op1.uriId());

            t.reset(false);
        }

        t.mEditor.removeCellAtPosition(4, t.mOps);
        {
            t.assertItemsNotChanged();
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is a delete of the cell
            assertEquals(OperationWrapper.TYPE_DELETE, op1.mType);
            assertEquals(2L, op1.uriId());

            t.reset(false);
        }

        t.mEditor.removeCellAtPosition(5, t.mOps);
        {
            t.assertItemsNotChanged();
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an delete of the row
            assertEquals(OperationWrapper.TYPE_DELETE, op1.mType);
            assertEquals(5L, op1.uriId());

            t.reset(false);
        }

        t.mEditor.removeCellAtPosition(6, t.mOps);
        {
            t.assertItemsNotChanged();
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an delete of the row
            assertEquals(OperationWrapper.TYPE_DELETE, op1.mType);
            assertEquals(8L, op1.uriId());

            t.reset(false);
        }

        t.mEditor.removeCellAtPosition(7, t.mOps);
        {
            t.assertItemsNotChanged();
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an delete of the row
            assertEquals(OperationWrapper.TYPE_DELETE, op1.mType);
            assertEquals(9L, op1.uriId());

            t.reset(false);
        }

        t.mEditor.removeCellAtPosition(8, t.mOps);
        t.assertItemsNotChanged();
        assertEquals(0, t.opsSize());
        t.reset(false);


    }

    private int getIntFrom(OperationWrapper op, String field) {
        return op.mContentValues.getAsInteger(field).intValue();
    }

    @android.test.UiThreadTest
    public void testIncreaseSpan_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(2).build()
        );

        t.mEditor.increaseSpan(0, t.mOps);
        {
            // should have 1 operation, the update of the cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(1L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseSpan(1, t.mOps);
        {
            // should have 1 operation, the update of the cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(3, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(2L, op1.uriId());

            t.reset();
        }

    }


    @android.test.UiThreadTest
    public void testIncreaseRowHeight_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(4L).row(1L, 1).position(0).height(2).build(), // 0
                buildHomeItem(5L).row(1L, 1).position(0).height(2).build(), // 1
                buildHomeItem(1L).row(2L, 2).position(0).build(), // 2
                buildHomeItem(7L).row(2L, 2).position(0).build(), // 3
                buildHomeItem(2L).row(2L, 2).position(1).colspan(2).build(), // 4
                buildHomeItem(8L).row(3L, 3).position(0).build(), // 5
                buildHomeItem(9L).row(3L, 3).position(0).build() // 6
        );

        t.mEditor.increaseHeight(0, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(3, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(1L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseHeight(1, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(3, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(1L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseHeight(2, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(2L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseHeight(3, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(2L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseHeight(4, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(2L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseHeight(5, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(3L, op1.uriId());

            t.reset();
        }

        t.mEditor.increaseHeight(6, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(3L, op1.uriId());

            t.reset();
        }

    }

    @android.test.UiThreadTest
    public void testIncreaseRowHeight_invalidIdx() {
        TestState t = new TestState(mContext,
                buildHomeItem(4L).row(1L, 1).position(0).height(2).build(), // 0
                buildHomeItem(5L).row(1L, 1).position(0).height(2).build(), // 1
                buildHomeItem(1L).row(2L, 2).position(0).build(), // 2
                buildHomeItem(7L).row(2L, 2).position(0).build(), // 3
                buildHomeItem(2L).row(2L, 2).position(1).colspan(2).build(), // 4
                buildHomeItem(8L).row(3L, 3).position(0).build(), // 5
                buildHomeItem(9L).row(3L, 3).position(0).build() // 6
        );

        t.mEditor.increaseHeight(-1, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.increaseHeight(7, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

    }


    @android.test.UiThreadTest
    public void testDecreaseRowHeight_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(4L).row(1L, 1).position(0).height(2).build(), // 0
                buildHomeItem(5L).row(1L, 1).position(0).height(2).build(), // 1
                buildHomeItem(1L).row(2L, 2).position(0).build(), // 2
                buildHomeItem(7L).row(2L, 2).position(0).build(), // 3
                buildHomeItem(2L).row(2L, 2).position(1).colspan(2).build(), // 4
                buildHomeItem(8L).row(3L, 3).position(0).build(), // 5
                buildHomeItem(9L).row(3L, 3).position(0).build() // 6
        );

        t.mEditor.decreaseHeight(0, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(1L, op1.uriId());

            t.reset();
        }

        t.mEditor.decreaseHeight(1, t.mOps);
        {
            // should have 1 operation, the update of the row
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, HomeContract.Rows.HEIGHT));
            assertEquals(1L, op1.uriId());

            t.reset();
        }

        t.mEditor.decreaseHeight(2, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.decreaseHeight(3, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.decreaseHeight(4, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.decreaseHeight(5, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.decreaseHeight(6, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();


    }

    @android.test.UiThreadTest
    public void testDecreaseRowHeight_invalidIdx() {
        TestState t = new TestState(mContext,
                buildHomeItem(4L).row(1L, 1).position(0).height(2).build(), // 0
                buildHomeItem(5L).row(1L, 1).position(0).height(2).build(), // 1
                buildHomeItem(1L).row(2L, 2).position(0).build(), // 2
                buildHomeItem(7L).row(2L, 2).position(0).build(), // 3
                buildHomeItem(2L).row(2L, 2).position(1).colspan(2).build(), // 4
                buildHomeItem(8L).row(3L, 3).position(0).build(), // 5
                buildHomeItem(9L).row(3L, 3).position(0).build() // 6
        );

        t.mEditor.decreaseHeight(-1, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.decreaseHeight(7, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellLeftOf_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(5L).row(3L, 1).position(0).build(),
                buildHomeItem(1L).row(2L, 2).position(0).build(),
                buildHomeItem(2L).row(2L, 2).position(1).colspan(2).build(),
                buildHomeItem(8L).row(1L, 3).position(0).build()
        );

        t.mEditor.insertCellLeftOfPosition(-1, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(0, t.mOps);
        {
            // should have 1 operation,
            // the update of the cell 5L
            // The insert of the new cell
            assertEquals(2, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);
            OperationWrapper op2 = t.wrapperAt(1);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.POSITION));
            assertEquals(5L, op1.uriId());

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
            assertEquals(0, getIntFrom(op2, Cells.POSITION));
            assertEquals(3L, getLongFrom(op2, Cells.ROW_ID));

            t.reset();
        }


        t.mEditor.insertCellLeftOfPosition(1, t.mOps);
        {
            // should have 3 operation,
            // the update of the cell 1L
            // the update of the cell 2L
            // The insert of the new cell
            assertEquals(3, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);
            OperationWrapper op2 = t.wrapperAt(1);
            OperationWrapper op3 = t.wrapperAt(2);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.POSITION));
            assertEquals(1L, op1.uriId());

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op2.mType);
            assertEquals(2, getIntFrom(op2, Cells.POSITION));
            assertEquals(2L, op2.uriId());

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_INSERT, op3.mType);
            assertEquals(0, getIntFrom(op3, Cells.POSITION));
            assertEquals(2L, getLongFrom(op3, Cells.ROW_ID));

            t.reset();
        }

        t.mEditor.insertCellLeftOfPosition(2, t.mOps);
        {
            // should have 2 operation,
            // the update of the cell 2L
            // The insert of the new cell
            assertEquals(2, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);
            OperationWrapper op2 = t.wrapperAt(1);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.POSITION));
            assertEquals(2L, op1.uriId());

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
            assertEquals(1, getIntFrom(op2, Cells.POSITION));
            assertEquals(2L, getLongFrom(op2, Cells.ROW_ID));

            t.reset();
        }

        t.mEditor.insertCellLeftOfPosition(3, t.mOps);
        {
            // should have 2 operation,
            // the update of the cell 2L
            // The insert of the new cell
            assertEquals(2, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);
            OperationWrapper op2 = t.wrapperAt(1);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.POSITION));
            assertEquals(8L, op1.uriId());

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
            assertEquals(0, getIntFrom(op2, Cells.POSITION));
            assertEquals(1L, getLongFrom(op2, Cells.ROW_ID));

            t.reset();
        }

        t.mEditor.insertCellLeftOfPosition(4, t.mOps);
        // should have 0 operation as it is an invalid position
        assertEquals(0, t.opsSize());
        t.reset();

    }

    private long getLongFrom(OperationWrapper op, String field) {
        return op.mContentValues.getAsLong(field).longValue();
    }

    @android.test.UiThreadTest
    public void testInsertCellLeftOf_noSpaceV1() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(2).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(2).build()
        );

        t.mEditor.insertCellLeftOfPosition(0, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(1, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(2, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellLeftOf_noSpaceV2() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(3).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build()
        );

        t.mEditor.insertCellLeftOfPosition(0, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(1, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(2, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellLeftOf_noSpaceV3() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(1).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build(),
                buildHomeItem(3L).row(1L, 1).position(2).colspan(1).build(),
                buildHomeItem(4L).row(1L, 1).position(3).colspan(1).build()
        );

        t.mEditor.insertCellLeftOfPosition(0, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(1, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellLeftOfPosition(2, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellRightOf_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(5L).row(3L, 1).position(0).build(),
                buildHomeItem(1L).row(2L, 2).position(0).build(),
                buildHomeItem(2L).row(2L, 2).position(1).colspan(2).build(),
                buildHomeItem(8L).row(1L, 3).position(0).build()
        );

        t.mEditor.insertCellRightOfPosition(-1, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(0, t.mOps);
        {
            // should have 1 operation,
            // The insert of the new cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_INSERT, op1.mType);
            assertEquals(3L, getLongFrom(op1, Cells.ROW_ID));
            assertEquals(1, getIntFrom(op1, Cells.POSITION));

            t.reset();
        }

        t.mEditor.insertCellRightOfPosition(1, t.mOps);
        {
            // should have 2 operation,
            // the update of the cell 2L
            // The insert of the new cell
            assertEquals(2, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);
            OperationWrapper op2 = t.wrapperAt(1);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.POSITION));
            assertEquals(2L, op1.uriId());

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
            assertEquals(1, getIntFrom(op2, Cells.POSITION));
            assertEquals(2L, getLongFrom(op2, Cells.ROW_ID));

            t.reset();
        }

        t.mEditor.insertCellRightOfPosition(2, t.mOps);
        {
            // should have 1 operation,
            // The insert of the new cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert of the cell
            assertEquals(OperationWrapper.TYPE_INSERT, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.POSITION));
            assertEquals(2L, getLongFrom(op1, Cells.ROW_ID));

            t.reset();
        }
        t.mEditor.insertCellRightOfPosition(3, t.mOps);
        {
            // should have 1 operation,
            // The insert of the new cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert of the cell
            assertEquals(OperationWrapper.TYPE_INSERT, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.POSITION));
            assertEquals(1L, getLongFrom(op1, Cells.ROW_ID));

            t.reset();
        }

        t.mEditor.insertCellRightOfPosition(4, t.mOps);
        // should have 0 operation as it is an invalid position
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellRightOf_noSpaceV1() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(2).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(2).build()
        );

        t.mEditor.insertCellRightOfPosition(0, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(1, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(2, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellRightOf_noSpaceV2() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(3).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build()
        );

        t.mEditor.insertCellRightOfPosition(0, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(1, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(2, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testInsertCellRightOf_noSpaceV3() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(1).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build(),
                buildHomeItem(3L).row(1L, 1).position(2).colspan(1).build(),
                buildHomeItem(4L).row(1L, 1).position(1).colspan(1).build()
        );

        t.mEditor.insertCellRightOfPosition(0, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(1, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.insertCellRightOfPosition(2, t.mOps);
        // should have 0 operation,
        assertEquals(0, t.opsSize());
        t.reset();

    }

    @android.test.UiThreadTest
    public void testIncreaseSpan_singleItem() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build()
        );

        t.mEditor.increaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());
    }

    @android.test.UiThreadTest
    public void testIncreaseSpan_multipleRows() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(1).build(), // 0
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build(), // 1
                buildHomeItem(3L).row(2L, 2).position(0).colspan(2).build(), // 2
                buildHomeItem(4L).row(2L, 2).position(1).colspan(2).build(), // 3
                buildHomeItem(5L).row(3L, 3).position(0).colspan(1).build(), // 4
                buildHomeItem(6L).row(3L, 3).position(1).colspan(1).build(), // 5
                buildHomeItem(11L).row(3L, 3).position(2).colspan(1).build(), // 6
                buildHomeItem(7L).row(4L, 4).position(0).colspan(3).build(), // 7
                buildHomeItem(9L).row(5L, 5).position(0).colspan(2).build(), // 8
                buildHomeItem(10L).row(5L, 5).position(1).colspan(1).build() // 9
        );

        t.mEditor.increaseSpan(2, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.increaseSpan(3, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        {
            t.mEditor.increaseSpan(4, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(5L, op1.uriId());
            t.reset();
        }

        {
            t.mEditor.increaseSpan(5, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(6L, op1.uriId());
            t.reset();
        }

        {
            t.mEditor.increaseSpan(6, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(2, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(11L, op1.uriId());
            t.reset();
        }
        {
            t.mEditor.increaseSpan(7, t.mOps);
            assertEquals(0, t.opsSize());
            t.reset();
        }
        {
            t.mEditor.increaseSpan(8, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(3, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(9L, op1.uriId());
            t.reset();
        }
    }

    @android.test.UiThreadTest
    public void testIncreaseSpan_maxSpan() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(3).build()
        );

        t.mEditor.increaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.increaseSpan(1, t.mOps);
        assertEquals(0, t.opsSize());

        t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(2).build(),
                buildHomeItem(3L).row(1L, 1).position(2).colspan(1).build()
        );

        t.mEditor.increaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.increaseSpan(1, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.increaseSpan(2, t.mOps);
        assertEquals(0, t.opsSize());
    }

    @android.test.UiThreadTest
    public void testIncreaseSpan_maxItems() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).build(),
                buildHomeItem(3L).row(1L, 1).position(2).build(),
                buildHomeItem(4L).row(1L, 1).position(3).build()
        );

        t.mEditor.increaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.increaseSpan(1, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.increaseSpan(2, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.increaseSpan(3, t.mOps);
        assertEquals(0, t.opsSize());

    }

    @android.test.UiThreadTest
    public void testDecreaseSpan_normal() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(2).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(2).build()
        );

        t.mEditor.decreaseSpan(0, t.mOps);
        {
            // should have 1 operation, the update of the cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(1L, op1.uriId());

            t.reset();
        }

        t.mEditor.decreaseSpan(1, t.mOps);
        {
            // should have 1 operation, the update of the cell
            assertEquals(1, t.opsSize());

            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an insert or the row
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(2L, op1.uriId());

            t.reset();
        }

    }

    // TODO: add case that the span is reset when only one cell is left after a delete

    @android.test.UiThreadTest
    public void testDecreaseSpan_singleItem() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(2).build()
        );

        t.mEditor.decreaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());
    }

    @android.test.UiThreadTest
    public void testDecreaseSpan_multipleRows() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).colspan(1).build(), // 0
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build(), // 1
                buildHomeItem(3L).row(2L, 2).position(0).colspan(2).build(), // 2
                buildHomeItem(4L).row(2L, 2).position(1).colspan(2).build(), // 3
                buildHomeItem(5L).row(3L, 3).position(0).colspan(1).build(), // 4
                buildHomeItem(6L).row(3L, 3).position(1).colspan(1).build(), // 5
                buildHomeItem(11L).row(3L, 3).position(2).colspan(2).build(), // 6
                buildHomeItem(7L).row(4L, 4).position(0).colspan(3).build(), // 7
                buildHomeItem(9L).row(5L, 5).position(0).colspan(2).build(), // 8
                buildHomeItem(10L).row(5L, 5).position(1).colspan(1).build() // 9
        );

        t.mEditor.decreaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();

        t.mEditor.decreaseSpan(1, t.mOps);
        assertEquals(0, t.opsSize());
        t.reset();


        {
            t.mEditor.decreaseSpan(2, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(3L, op1.uriId());
            t.reset();
        }

        {
            t.mEditor.decreaseSpan(3, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(4L, op1.uriId());
            t.reset();
        }

        {
            t.mEditor.decreaseSpan(4, t.mOps);
            assertEquals(0, t.opsSize());
            t.reset();
        }

        {
            t.mEditor.decreaseSpan(5, t.mOps);
            assertEquals(0, t.opsSize());
            t.reset();
        }

        {
            t.mEditor.decreaseSpan(6, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(11L, op1.uriId());
            t.reset();
        }
        {
            t.mEditor.decreaseSpan(7, t.mOps);
            assertEquals(0, t.opsSize());
            t.reset();
        }
        {
            t.mEditor.decreaseSpan(8, t.mOps);
            assertEquals(1, t.opsSize());
            OperationWrapper op1 = t.wrapperAt(0);

            // check that this is an update of the cell
            assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
            assertEquals(1, getIntFrom(op1, Cells.COLSPAN));
            assertEquals(9L, op1.uriId());
            t.reset();
        }
        {
            t.mEditor.decreaseSpan(9, t.mOps);
            assertEquals(0, t.opsSize());
            t.reset();
        }
    }

    @android.test.UiThreadTest
    public void testDecreaseSpan_maxSpan() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(1).build()
        );

        t.mEditor.decreaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.decreaseSpan(1, t.mOps);
        assertEquals(0, t.opsSize());

        t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).colspan(2).build(),
                buildHomeItem(3L).row(1L, 1).position(2).colspan(1).build()
        );

        t.mEditor.decreaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.decreaseSpan(1, t.mOps);
        assertEquals(1, t.opsSize());

        t.reset();
        t.mEditor.decreaseSpan(2, t.mOps);
        assertEquals(0, t.opsSize());
    }

    @android.test.UiThreadTest
    public void testDecreaseSpan_maxItems() {
        TestState t = new TestState(mContext,
                buildHomeItem(1L).row(1L, 1).position(0).build(),
                buildHomeItem(2L).row(1L, 1).position(1).build(),
                buildHomeItem(3L).row(1L, 1).position(2).build(),
                buildHomeItem(4L).row(1L, 1).position(3).build()
        );

        t.mEditor.decreaseSpan(0, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.decreaseSpan(1, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.decreaseSpan(2, t.mOps);
        assertEquals(0, t.opsSize());

        t.reset();
        t.mEditor.decreaseSpan(3, t.mOps);
        assertEquals(0, t.opsSize());

    }

    @android.test.UiThreadTest
    public void testAddRowBelow_singleRow() {
        TestState t = new TestState(mContext,
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */)
        );

        t.mEditor.insertRowBelowPosition(0, t.mOps);
        // should have 2 operation, the insert of the row below
        // and the cell in that row
        assertEquals(2, t.opsSize());

        OperationWrapper op1 = t.wrapperAt(0);
        OperationWrapper op2 = t.wrapperAt(1);

        // check that this is an insert or the row
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(2, getIntFrom(op1, HomeContract.Rows.POSITION));

        // check that this is an insert of the cell, with a back-ref to row 0
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(0, op2.mBackReferences.get(Cells._ROW_ID).intValue());
    }

    HomeItem createHomeItem(long id, long rowId, int rowPosition, int cellPosition) {
        HomeItem homeItem = new HomeItem();
        homeItem.mId = id;
        homeItem.mRowId = rowId;
        homeItem.mRowPosition = rowPosition;
        homeItem.mPosition = cellPosition;
        homeItem.mRowHeight = 1;
        homeItem.mColspan = 1;
        homeItem.mPageId = 1;

        return homeItem;
    }

    @android.test.UiThreadTest
    public void testAddRowBelow_negativeIdx() {
        TestState t = new TestState(mContext,
                createHomeItem(0 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */)
        );

        // Now try an invalid position, this should result in an empty
        // insert
        t.mEditor.insertRowBelowPosition(-1, t.mOps);
        assertEquals(0, t.opsSize());
    }

    @android.test.UiThreadTest
    public void testAddRowBelow_invalidIdx() {
        TestState t = new TestState(mContext,
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */)
        );

        // Now try an invalid position, this should result in an empty
        // insert
        t.mEditor.insertRowBelowPosition(1, t.mOps);
        assertEquals(0, t.opsSize());

    }

    @android.test.UiThreadTest
    public void testAddRowAbove_betweenRows() {
        TestState t = new TestState(mContext,
                createHomeItem(0 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */),
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 1 /* x */),
                createHomeItem(2 /* id */, 2 /* rowId */, 2 /* y */, 0 /* x */)
        );

        t.mEditor.insertRowAbovePosition(2, t.mOps);
        // should have 3 operation, the move of the row below,
        // the insert of the new row and the new cell in that row
        assertEquals(3, t.opsSize());

        OperationWrapper op1 = t.wrapperAt(0);
        OperationWrapper op2 = t.wrapperAt(1);
        OperationWrapper op3 = t.wrapperAt(2);

        // check that this is an update from position 2 to position 3
        assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
        assertEquals(2L, op1.uriId());
        assertEquals(3, getIntFrom(op1, HomeContract.Rows.POSITION));

        // check that this is an insert of a row at position 2
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(2, getIntFrom(op2, HomeContract.Rows.POSITION));

        // check that this is an insert
        assertEquals(OperationWrapper.TYPE_INSERT, op3.mType);
        assertEquals(1, op3.mBackReferences.get(Cells._ROW_ID).intValue());

    }

    @android.test.UiThreadTest
    public void testAddRowBelow_betweenRows() {
        TestState t = new TestState(mContext,
                createHomeItem(0 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */),
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 1 /* x */),
                createHomeItem(2 /* id */, 2 /* rowId */, 2 /* y */, 0 /* x */)
        );

        t.mEditor.insertRowBelowPosition(1, t.mOps);
        // should have 3 operation, the insert of the row below
        // and the cell in that row
        assertEquals(3, t.opsSize());

        OperationWrapper op1 = t.wrapperAt(0);
        OperationWrapper op2 = t.wrapperAt(1);
        OperationWrapper op3 = t.wrapperAt(2);

        // check that this is an update from position 2 to position 3
        assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
        assertEquals(2L, op1.uriId());
        assertEquals(3, getIntFrom(op1, HomeContract.Rows.POSITION));

        // check that this is an insert of a row at position 2
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(2, getIntFrom(op2, HomeContract.Rows.POSITION));

        // check that this is an insert
        assertEquals(OperationWrapper.TYPE_INSERT, op3.mType);
        assertEquals(1, op3.mBackReferences.get(Cells._ROW_ID).intValue());
    }

    @android.test.UiThreadTest
    public void testAddRowBelow_betweenRowsAlternatePosition() {
        TestState t = new TestState(mContext,
                createHomeItem(0 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */),
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 1 /* x */),
                createHomeItem(2 /* id */, 2 /* rowId */, 2 /* y */, 0 /* x */)
        );

        t.mEditor.insertRowBelowPosition(0, t.mOps);
        // should have 2 operation, the insert of the row below
        // and the cell in that row
        assertEquals(3, t.opsSize());

        OperationWrapper op1 = t.wrapperAt(0);
        OperationWrapper op2 = t.wrapperAt(1);
        OperationWrapper op3 = t.wrapperAt(2);

        // check that this is an update from position 2 to position 3
        assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
        assertEquals(2L, op1.uriId());
        assertEquals(3, getIntFrom(op1, HomeContract.Rows.POSITION));

        // check that this is an insert of a row at position 2
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(2, getIntFrom(op2, HomeContract.Rows.POSITION));

        // check that this is an insert
        assertEquals(OperationWrapper.TYPE_INSERT, op3.mType);
        assertEquals(1, op3.mBackReferences.get(Cells._ROW_ID).intValue());


    }

    /**
     * same as testAddRowAbove_betweenRows, but performs the insert before position 3
     * to see if it changes this internally to 2
     */
    @android.test.UiThreadTest
    public void testAddRowAbove_betweenRowsAlternatePosition() {
        TestState t = new TestState(mContext,
                createHomeItem(0 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */),
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 1 /* x */),
                createHomeItem(2 /* id */, 2 /* rowId */, 2 /* y */, 0 /* x */),
                createHomeItem(3 /* id */, 2 /* rowId */, 2 /* y */, 1 /* x */)
        );

        t.mEditor.insertRowAbovePosition(3, t.mOps);
        // should have 2 operation, the insert of the row below
        // and the cell in that row
        assertEquals(3, t.opsSize());

        OperationWrapper op1 = t.wrapperAt(0);
        OperationWrapper op2 = t.wrapperAt(1);
        OperationWrapper op3 = t.wrapperAt(2);

        // check that this is an update from position 2 to position 3 (1 based)
        assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
        assertEquals(2L, op1.uriId());
        assertEquals(3, getIntFrom(op1, HomeContract.Rows.POSITION));

        // check that this is an insert of a row at position 2
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(2, getIntFrom(op2, HomeContract.Rows.POSITION));

        // check that this is an insert
        assertEquals(OperationWrapper.TYPE_INSERT, op3.mType);
        assertEquals(1, op3.mBackReferences.get(Cells._ROW_ID).intValue());


    }

    @android.test.UiThreadTest
    public void testAddRowAbove_singleRow() {
        TestState t = new TestState(mContext,
                createHomeItem(0 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */)
        );

        t.mEditor.insertRowAbovePosition(0, t.mOps);
        // should have 1 operation; The update of the existing row;
        // The insert of the new row and the insert of a new cell into
        // that row
        assertEquals(3, t.opsSize());

        OperationWrapper op1 = t.wrapperAt(0);
        OperationWrapper op2 = t.wrapperAt(1);
        OperationWrapper op3 = t.wrapperAt(2);

        // check that this is an update from position 1 to position two
        assertEquals(OperationWrapper.TYPE_UPDATE, op1.mType);
        assertEquals(2, getIntFrom(op1, HomeContract.Rows.POSITION));

        // check that this is an insert
        assertEquals(OperationWrapper.TYPE_INSERT, op2.mType);
        assertEquals(1, getIntFrom(op2, HomeContract.Rows.POSITION));

        // check that this is an insert
        assertEquals(OperationWrapper.TYPE_INSERT, op3.mType);
        assertEquals(1, op3.mBackReferences.get(Cells._ROW_ID).intValue());

    }

    @android.test.UiThreadTest
    public void testAddRowAbove_negativeIdx() {
        TestState t = new TestState(mContext,
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */)
        );

        // Now try an invalid position, this should result in an empty
        // insert
        t.reset();
        t.mEditor.insertRowAbovePosition(-1, t.mOps);
        assertEquals(0, t.opsSize());

    }

    @android.test.UiThreadTest
    public void testAddRowAbove_invalidIdx() {
        TestState t = new TestState(mContext,
                createHomeItem(1 /* id */, 1 /* rowId */, 1 /* y */, 0 /* x */)
        );


        // Now try an invalid position, this should result in an empty
        // insert
        t.reset();
        t.mEditor.insertRowAbovePosition(1, t.mOps);
        assertEquals(0, t.opsSize());

    }

    static class HomeItemBuilder {

        final long mId;

        long mRowId;

        int mRowPosition;

        int mPosition;

        int mHeight = 1;

        int mColspan = 1;

        long mPageId = 1;

        HomeItemBuilder(long id) {
            mId = id;
        }

        HomeItemBuilder row(long rowId, int rowPosition) {
            mRowId = rowId;
            mRowPosition = rowPosition;
            return this;
        }

        HomeItemBuilder colspan(int span) {
            mColspan = span;
            return this;
        }

        HomeItemBuilder position(int position) {
            mPosition = position;
            return this;
        }

        HomeItemBuilder height(int height) {
            mHeight = height;
            return this;
        }

        HomeItemBuilder pageId(long pageId) {
            mPageId = pageId;
            return this;
        }

        HomeItem build() {
            HomeItem homeItem = new HomeItem();
            homeItem.mId = mId;
            homeItem.mRowId = mRowId;
            homeItem.mRowPosition = mRowPosition;
            homeItem.mPosition = mPosition;
            homeItem.mRowHeight = mHeight;
            homeItem.mColspan = mColspan;
            homeItem.mPageId = mPageId;
            return homeItem;
        }

    }

    static class TestState {

        final HomeAdapter mHomeAdapter;

        final HomeAdapter.HomeAdapterEditor mEditor;

        final ArrayList<ContentProviderOperation> mOps;

        final List<OperationWrapper> mOpWrappers;

        final TestOpsBuilder mTestOpsBuilder;

        final List<HomeItem> mInternalList;

        TestState(Context context, HomeItem... homeItems) {
            mHomeAdapter = new HomeAdapter(context, 1);
            mEditor = mHomeAdapter.getEditor();

            mOps = new ArrayList<>();
            mTestOpsBuilder = new TestOpsBuilder();
            mEditor.mOpsBuilder = mTestOpsBuilder;
            mOpWrappers = mTestOpsBuilder.mOperationWrappers;

            List<HomeItem> itemList = Arrays.asList(homeItems);
            mHomeAdapter.setHomeItems(itemList);

            mInternalList = new ArrayList<>(homeItems.length);
            for (HomeItem homeItem : homeItems) {
                mInternalList.add(new HomeItem(homeItem));
            }
        }

        int opsSize() {
            assertEquals(mOps.size(), mOpWrappers.size());
            return mOps.size();
        }

        OperationWrapper wrapperAt(int position) {
            return mOpWrappers.get(position);
        }

        void reset() {
            reset(true);
        }

        void reset(boolean assertNotModified) {
            if (assertNotModified) {
                assertItemsNotChanged();
            }
            mTestOpsBuilder.reset();
            mOps.clear();
        }

        void assertItemsNotChanged() {
            int N = mInternalList.size();
            for (int i = 0; i < N; i++) {
                HomeItem original = mInternalList.get(i);
                HomeItem item = mHomeAdapter.getItemAt(i);
                assertHomeItemNotChanged(original, item);
            }
        }

        private void assertHomeItemNotChanged(HomeItem original, HomeItem item) {
            assertEquals("HomeItem field 'mId' was changed",
                    original.mId, item.mId);

            assertEquals("HomeItem field 'mColSpan' was changed",
                    original.mColspan, item.mColspan);

            assertEquals("HomeItem field 'mDisplayType' was changed",
                    original.mDisplayType, item.mDisplayType);

            assertEquals("HomeItem field 'mPageId' was changed",
                    original.mPageId, item.mPageId);

            assertEquals("HomeItem field 'mPosition' was changed",
                    original.mPosition, item.mPosition);

            assertEquals("HomeItem field 'mRowPosition' was changed",
                    original.mRowPosition, item.mRowPosition);

            assertEquals("HomeItem field 'mRowId' was changed",
                    original.mRowId, item.mRowId);

            assertEquals("HomeItem field 'mRowHeight' was changed",
                    original.mRowHeight, item.mRowHeight);

            assertEquals("HomeItem field 'mPageName' was changed",
                    original.mPageName, item.mPageName);
        }

    }

}
