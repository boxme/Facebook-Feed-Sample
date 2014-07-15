package com.desmond.facebook_like_feed;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

/**
 * Created by desmond on 13/7/14.
 */
public class FeedImageView extends ImageView {

    private ResponseObserver mObserver;
    private String mUrl;
    private int mDefaultImageId;
    private int mErrorImageId;
    private ImageLoader mImageLoader;
    private ImageLoader.ImageContainer mImageContainer;

    public interface ResponseObserver {
        public void onError();
        public void onSuccess();
    }

    public FeedImageView(Context context) {
        this(context, null);
    }

    public FeedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FeedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setResponseObserver(ResponseObserver observer) {
        mObserver = observer;
    }

    /**
     * Sets URL of the image that should be loaded into this view. Note that
     * calling this will immediately either set the cached image (if any)
     * or the default image specified by
     * {@link FeedImageView#setDefaultImageResId(int)} on the view.
     *
     * Note: If applicable, {@link com.desmond.facebook_like_feed.FeedImageView#setDefaultImageResId(int)}
     * and {@link com.desmond.facebook_like_feed.FeedImageView#setErrorImageResId(int)} should be called
     * prior to calling this function.
     *
     * @param url The Url that should be loaded into this ImageView
     * @param imageLoader ImageLoader that will be used to make the request.
     */
    public void setImageUrl(String url, ImageLoader imageLoader) {
        mUrl = url;
        mImageLoader = imageLoader;
        //The URL might have changed, see if we need to load it
        loadImageIfNecessary(false);
    }

    /**
     * Sets the default image resource ID to be sued for this view until the
     * attempt to load it completes
     * @param defaultImage
     */
    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    /**
     * Sets the error image resource ID to be used for this view in the event
     * that the image requested fails to load
     * @param errorImage
     */
    public void setErrorImageResId(int errorImage) {
        mErrorImageId = errorImage;
    }

    /**
     * Loads the image for the view if it isn't already loaded
     *
     * @param isInLayoutPass True if this was invoked from a
     *                       layout pass, false otherwise
     */
    private void loadImageIfNecessary(final boolean isInLayoutPass) {
        final int width = getWidth();
        int height = getHeight();

        boolean isFullyWrapContent = getLayoutParams() != null
                && getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT
                && getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT;

        //if the view's bounds aren't known yet, and this is not a
        //wrap-content/wrap-content view, hold off on loading the image
        if (width == 0 && height == 0 && !isFullyWrapContent)
            return;

        //If the URL to be loaded in this view is empty, cancel any old
        //requests and clear the currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        //If there was an old request in this view, check if it needs to be
        //canceled
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mUrl)) {
                return;
            } else {
                //If there is a pre-existing request, cancel it if it's fetching
                //a different url
                mImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }

        //The pre-existing content of this view didn't match the current Url
        //Load the new image from the network
        ImageLoader.ImageContainer newContainer = mImageLoader.get(mUrl,
                new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                        //If this was an immediate response that was delivered inside of a layout
                        //pass do not set the image immediately as it will trigger a requestLayout
                        //inside of a layout. Instead, defer setting the image by posting back
                        //to the main thread
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    onResponse(response, false);
                                }
                            });
                            return;
                        }

                        int bWidth = 0, bHeight = 0;
                        if (response.getBitmap() != null) {
                            setImageBitmap(response.getBitmap());
                            bWidth = response.getBitmap().getWidth();
                            bHeight = response.getBitmap().getHeight();
                            adjustImageAspect(bWidth, bHeight);

                        } else if (mDefaultImageId != 0) {
                            setImageResource(mDefaultImageId);
                        }

                        if (mObserver != null) {
                            mObserver.onSuccess();
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (mErrorImageId != 0) {
                            setImageResource(mErrorImageId);
                        }

                        if (mObserver != null) {
                            mObserver.onError();
                        }
                    }
                });

        //Update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    private void setDefaultImageOrNull() {
        if (mDefaultImageId != 0) {
            setImageResource(mDefaultImageId);
        } else {
            setImageBitmap(null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            //If the view was bound to an image request, cancel it and
            //clear out the image from the view
            mImageContainer.cancelRequest();
            setImageBitmap(null);

            //Also clear out the container so that we can reload the image
            //if necessary
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    /**
     * Key different between this imageView and NetworkImageView provided by Volley
     *
     * Adjusting imageView height
     */
    private void adjustImageAspect(int bWidth, int bHeight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();

        if (bWidth == 0 || bHeight == 0)
            return;

        int sWidth = getWidth();
        int new_height = 0;
        new_height = sWidth * bHeight / bWidth;
        params.width = sWidth;
        params.height = new_height;
        setLayoutParams(params);
    }
}
