package com.desmond.facebook_like_feed.adapter;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.desmond.facebook_like_feed.FeedImageView;
import com.desmond.facebook_like_feed.R;
import com.desmond.facebook_like_feed.app.AppController;
import com.desmond.facebook_like_feed.data.FeedItem;

import java.util.List;

/**
 * Created by desmond on 14/7/14.
 */
public class FeedListAdapter extends BaseAdapter {

    private Context ctx;
    private List<FeedItem> feedItems;
    private ImageLoader imageLoader = AppController.getInstance().getImageLoader();

    public FeedListAdapter(Context ctx, List<FeedItem> feedItems) {
        this.ctx = ctx;
        this.feedItems = feedItems;
    }

    @Override
    public int getCount() {
        return feedItems.size();
    }

    @Override
    public Object getItem(int position) {
        return feedItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(ctx).inflate(R.layout.feed_item, null);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.timeStamp = (TextView) convertView.findViewById(R.id.timestamp);
            holder.statusMsg = (TextView) convertView.findViewById(R.id.txtStatusMsg);
            holder.url = (TextView) convertView.findViewById(R.id.txtUrl);
            holder.profilePic = (NetworkImageView) convertView.findViewById(R.id.profilePic);
            holder.feedImageView = (FeedImageView) convertView.findViewById(R.id.feedImage1);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();
        FeedItem item = feedItems.get(position);

        holder.name.setText(item.getName());

        // Converting timestamp into x ago format
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                Long.parseLong(item.getTimeStamp()),
                System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);
        holder.timeStamp.setText(timeAgo);

        // Chcek for empty status message
        if (!TextUtils.isEmpty(item.getStatus())) {
            holder.statusMsg.setText(item.getStatus());
            holder.statusMsg.setVisibility(View.VISIBLE);
        } else {
            // status is empty, remove from view
            holder.statusMsg.setVisibility(View.GONE);
        }

        if (item.getUrl() != null) {
            holder.url.setText(Html.fromHtml("<a href=\"" + item.getUrl() + "\">"
                    + item.getUrl() + "</a> "));

            //Making url clickable
            holder.url.setMovementMethod(LinkMovementMethod.getInstance());
            holder.url.setVisibility(View.VISIBLE);
        } else {
            holder.url.setVisibility(View.GONE);
        }

        //User profile picture
        holder.profilePic.setImageUrl(item.getProfilePic(), imageLoader);

        //Feed image
        if (item.getImge() != null) {
            holder.feedImageView.setImageUrl(item.getImge(), imageLoader);
            holder.feedImageView.setVisibility(View.VISIBLE);
            holder.feedImageView.setResponseObserver(new FeedImageView.ResponseObserver() {
                @Override
                public void onError() {
                }

                @Override
                public void onSuccess() {
                }
            });
        } else {
            holder.feedImageView.setVisibility(View.GONE);
        }

        return convertView;
    }

    private class ViewHolder {
        TextView name;
        TextView timeStamp;
        TextView statusMsg;
        TextView url;
        NetworkImageView profilePic;
        FeedImageView feedImageView;
    }
}
