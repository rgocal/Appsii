package com.appsimobile.appsii.unlock;

// Declare any non-default types here with import statements

import android.os.Bundle;

/** Example service interface */
interface IAppsiPlugin {

    /**
     * Request the remote service to perform license validation.
     * The service should encode it's response with the provided salt
     */
    Bundle verifyLicense(in Bundle input, String salt);

}