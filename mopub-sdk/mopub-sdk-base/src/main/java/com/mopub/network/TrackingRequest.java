// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.VastErrorCode;
import com.mopub.mobileads.VastMacroHelper;
import com.mopub.mobileads.VastTracker;
import com.mopub.mobileads.VastTrackerTwo;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.Response;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.HttpHeaderParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class TrackingRequest extends MoPubRequest<Void> {

    // Retrying may cause duplicate impressions
    private static final int ZERO_RETRIES = 0;

    public interface Listener extends Response.ErrorListener {
        void onResponse(@NonNull String url);
    }

    @Nullable private final TrackingRequest.Listener mListener;

    private TrackingRequest(@NonNull final Context context,
            @NonNull final String url,
            @Nullable final Listener listener) {
        super(context, url, listener);
        mListener = listener;
        setShouldCache(false);
        setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                ZERO_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    @Override
    protected Response<Void> parseNetworkResponse(final NetworkResponse networkResponse) {
        if (networkResponse.statusCode != 200) {
            return Response.error(
                    new MoPubNetworkError("Failed to log tracking request. Response code: "
                            + networkResponse.statusCode + " for url: " + getUrl(),
                            MoPubNetworkError.Reason.TRACKING_FAILURE));
        }
        return Response.success(null, HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    @Override
    public void deliverResponse(final Void aVoid) {
        if (mListener != null) {
            mListener.onResponse(getUrl());
        }
    }

    ///////////////////////////////////////////////////////////////
    // Static helper methods that can be used as utilities:
    //////////////////////////////////////////////////////////////

    public static void makeVastTrackingHttpRequest(
            @NonNull final List<VastTracker> vastTrackers,
            @Nullable final VastErrorCode vastErrorCode,
            @Nullable final Integer contentPlayHead,
            @Nullable final String assetUri,
            @Nullable final Context context) {
        Preconditions.checkNotNull(vastTrackers);

        List<String> trackers = new ArrayList<String>(vastTrackers.size());
        for (VastTracker vastTracker : vastTrackers) {
            if (vastTracker == null) {
                continue;
            }
            if (vastTracker.isTracked() && !vastTracker.isRepeatable()) {
                continue;
            }
            trackers.add(vastTracker.getContent());
            vastTracker.setTracked();
        }

        makeTrackingHttpRequest(
                new VastMacroHelper(trackers)
                        .withErrorCode(vastErrorCode)
                        .withContentPlayHead(contentPlayHead)
                        .withAssetUri(assetUri)
                        .getUris(),
                context
        );
    }

    public static void makeVastTrackingTwoHttpRequest(
            @NonNull final List<VastTrackerTwo> vastTrackers,
            @Nullable final VastErrorCode vastErrorCode,
            @Nullable final Integer contentPlayHead,
            @Nullable final String assetUri,
            @Nullable final Context context) {
        Preconditions.checkNotNull(vastTrackers);

        List<String> trackers = new ArrayList<String>(vastTrackers.size());
        for (VastTrackerTwo vastTracker : vastTrackers) {
            if (vastTracker == null) {
                continue;
            }
            if (vastTracker.isTracked() && !vastTracker.isRepeatable()) {
                continue;
            }
            trackers.add(vastTracker.getContent());
            vastTracker.setTracked();
        }

        makeTrackingHttpRequest(
                new VastMacroHelper(trackers)
                        .withErrorCode(vastErrorCode)
                        .withContentPlayHead(contentPlayHead)
                        .withAssetUri(assetUri)
                        .getUris(),
                context
        );
    }

    public static void makeTrackingHttpRequest(@Nullable final Iterable<String> urls,
            @Nullable final Context context,
            @Nullable final Listener listener) {
        if (urls == null || context == null) {
            return;
        }

        final RequestQueue requestQueue = Networking.getRequestQueue(context);
        for (final String url : urls) {
            if (TextUtils.isEmpty(url)) {
                continue;
            }

            final TrackingRequest.Listener internalListener = new TrackingRequest.Listener() {
                @Override
                public void onResponse(@NonNull String url) {
                    MoPubLog.log(CUSTOM, "Successfully hit tracking endpoint: " + url);
                    if (listener != null) {
                        listener.onResponse(url);
                    }
                }

                @Override
                public void onErrorResponse(final VolleyError volleyError) {
                    MoPubLog.log(CUSTOM, "Failed to hit tracking endpoint: " + url);
                    if (listener != null) {
                        listener.onErrorResponse(volleyError);
                    }
                }
            };
            final TrackingRequest trackingRequest = new TrackingRequest(context, url,
                    internalListener);
            requestQueue.add(trackingRequest);
        }
    }

    public static void makeTrackingHttpRequest(@Nullable final String url,
            @Nullable final Context context) {
        makeTrackingHttpRequest(url, context, null);
    }

    public static void makeTrackingHttpRequest(@Nullable final String url,
            @Nullable final Context context,
            @Nullable Listener listener) {
        if (!TextUtils.isEmpty(url)) {
            makeTrackingHttpRequest(Arrays.asList(url), context, listener);
        }
    }

    public static void makeTrackingHttpRequest(@Nullable final Iterable<String> urls,
            @Nullable final Context context) {
        makeTrackingHttpRequest(urls, context, null);
    }
}
