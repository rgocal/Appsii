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
 * /apps/dashclock/api/internal/IExtension.aidl
 */
package com.google.android.apps.dashclock.api.internal;

public interface IExtension extends android.os.IInterface {

    /**
     * Local-side IPC implementation stub class.
     */
    public static abstract class Stub extends android.os.Binder
            implements com.google.android.apps.dashclock.api.internal.IExtension {

        private static final java.lang.String DESCRIPTOR =
                "com.google.android.apps.dashclock.api.internal.IExtension";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.google.android.apps.dashclock.api.internal.IExtension
         * interface,
         * generating a proxy if needed.
         */
        public static com.google.android.apps.dashclock.api.internal.IExtension asInterface(
                android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) &&
                    (iin instanceof com.google.android.apps.dashclock.api.internal.IExtension))) {
                return ((com.google.android.apps.dashclock.api.internal.IExtension) iin);
            }
            return new com.google.android.apps.dashclock.api.internal.IExtension.Stub.Proxy(obj);
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
                case TRANSACTION_onInitialize: {
                    data.enforceInterface(DESCRIPTOR);
                    com.google.android.apps.dashclock.api.internal.IExtensionHost _arg0;
                    _arg0 =
                            com.google.android.apps.dashclock.api.internal.IExtensionHost.Stub
                                    .asInterface(
                                            data.readStrongBinder());
                    boolean _arg1;
                    _arg1 = (0 != data.readInt());
                    this.onInitialize(_arg0, _arg1);
                    return true;
                }
                case TRANSACTION_onUpdate: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    this.onUpdate(_arg0);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy
                implements com.google.android.apps.dashclock.api.internal.IExtension {

            private android.os.IBinder mRemote;

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
            public void onInitialize(com.google.android.apps.dashclock.api.internal.IExtensionHost
                    host, boolean isReconnect)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((host != null)) ? (host.asBinder()) : (null)));
                    _data.writeInt(((isReconnect) ? (1) : (0)));
                    mRemote.transact(Stub.TRANSACTION_onInitialize, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onUpdate(int reason) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(reason);
                    mRemote.transact(Stub.TRANSACTION_onUpdate, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_onInitialize = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);

        static final int TRANSACTION_onUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
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
    public void onInitialize(com.google.android.apps.dashclock.api.internal.IExtensionHost host,
            boolean isReconnect) throws android.os.RemoteException;

    public void onUpdate(int reason) throws android.os.RemoteException;
}
