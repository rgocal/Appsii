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

/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/romannurik/Code/android/dashclock/api/src/main/aidl/com/google/android
 * /apps/dashclock/api/internal/IExtensionHost.aidl
 */
package com.google.android.apps.dashclock.api.internal;

public interface IExtensionHost extends android.os.IInterface {

    /**
     * Local-side IPC implementation stub class.
     */
    abstract class Stub extends android.os.Binder
            implements com.google.android.apps.dashclock.api.internal.IExtensionHost {

        static final java.lang.String DESCRIPTOR =
                "com.google.android.apps.dashclock.api.internal.IExtensionHost";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.google.android.apps.dashclock.api.internal
         * .IExtensionHost
         * interface,
         * generating a proxy if needed.
         */
        public static com.google.android.apps.dashclock.api.internal.IExtensionHost asInterface(
                android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) &&
                    (iin instanceof com.google.android.apps.dashclock.api.internal
                            .IExtensionHost))) {
                return ((com.google.android.apps.dashclock.api.internal.IExtensionHost) iin);
            }
            return new com.google.android.apps.dashclock.api.internal.IExtensionHost.Stub.Proxy(
                    obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply,
                int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_publishUpdate: {
                    data.enforceInterface(DESCRIPTOR);
                    com.google.android.apps.dashclock.api.ExtensionData _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 =
                                com.google.android.apps.dashclock.api.ExtensionData.CREATOR
                                        .createFromParcel(
                                                data);
                    } else {
                        _arg0 = null;
                    }
                    this.publishUpdate(_arg0);
                    return true;
                }
                case TRANSACTION_addWatchContentUris: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String[] _arg0;
                    _arg0 = data.createStringArray();
                    this.addWatchContentUris(_arg0);
                    return true;
                }
                case TRANSACTION_setUpdateWhenScreenOn: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg0;
                    _arg0 = (0 != data.readInt());
                    this.setUpdateWhenScreenOn(_arg0);
                    return true;
                }
                case TRANSACTION_removeAllWatchContentUris: {
                    data.enforceInterface(DESCRIPTOR);
                    this.removeAllWatchContentUris();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy
                implements com.google.android.apps.dashclock.api.internal.IExtensionHost {

            private final android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            /**
             * Since there might be a case where new versions of DashClock use extensions running
             * old versions of the protocol (and thus old versions of this AIDL),
             * there are a few things
             * to keep in mind when editing this class:
             * <p/>
             * - Order of functions defined below matters. New methods added in new protocol
             * versions must
             * be added below all other methods.
             * - Do NOT modify a signature once a protocol version is finalized.
             */// Protocol version 1 below
            @Override
            public void publishUpdate(com.google.android.apps.dashclock.api.ExtensionData data)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((data != null)) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_publishUpdate, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void addWatchContentUris(java.lang.String[] contentUris)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStringArray(contentUris);
                    mRemote.transact(Stub.TRANSACTION_addWatchContentUris, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setUpdateWhenScreenOn(boolean updateWhenScreenOn)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(((updateWhenScreenOn) ? (1) : (0)));
                    mRemote.transact(Stub.TRANSACTION_setUpdateWhenScreenOn, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
// Protcol version 2 below

            @Override
            public void removeAllWatchContentUris() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_removeAllWatchContentUris, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_publishUpdate =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);

        static final int TRANSACTION_addWatchContentUris =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);

        static final int TRANSACTION_setUpdateWhenScreenOn =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);

        static final int TRANSACTION_removeAllWatchContentUris =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    }

    /**
     * Since there might be a case where new versions of DashClock use extensions running
     * old versions of the protocol (and thus old versions of this AIDL), there are a few things
     * to keep in mind when editing this class:
     * <p/>
     * - Order of functions defined below matters. New methods added in new protocol versions must
     * be added below all other methods.
     * - Do NOT modify a signature once a protocol version is finalized.
     */// Protocol version 1 below
    void publishUpdate(com.google.android.apps.dashclock.api.ExtensionData data) throws
            android.os.RemoteException;

    void addWatchContentUris(java.lang.String[] contentUris)
            throws android.os.RemoteException;

    void setUpdateWhenScreenOn(boolean updateWhenScreenOn) throws android.os.RemoteException;
// Protcol version 2 below

    void removeAllWatchContentUris() throws android.os.RemoteException;
}
