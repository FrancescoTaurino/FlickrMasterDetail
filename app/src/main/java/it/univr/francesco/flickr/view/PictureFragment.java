package it.univr.francesco.flickr.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;
import it.univr.francesco.flickr.controller.ExecutorIntentService;
import it.univr.francesco.flickr.model.Model;

public class PictureFragment extends android.app.Fragment implements AbstractFragment {
    private MVC mvc;

    private ImageView picture;
    private TextView comments_label;
    private ListView comments;

    private CustomBroacastReceiver customBroacastReceiver;
    private IntentFilter intentFilter;

    @Override @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override @UiThread
    public android.view.View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(R.layout.fragment_picture, container, false);

        customBroacastReceiver = new CustomBroacastReceiver();
        intentFilter = new IntentFilter(ExecutorIntentService.ACTION_SEND_BITMAP_PATH);

        picture = (ImageView) view.findViewById(R.id.picture);
        comments_label = (TextView) view.findViewById(R.id.comments_label);
        comments = (ListView) view.findViewById(R.id.comments);

        return view;
    }

    @Override @UiThread
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mvc = ((Flickr) getActivity().getApplication()).getMVC();

        onModelChanged();
    }

    @Override @UiThread
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(customBroacastReceiver, intentFilter);
    }

    @Override @UiThread
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(customBroacastReceiver);
    }

    @Override @UiThread
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_share, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override @UiThread
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share_picture:
                if(mvc.model.getPictureFromCache(mvc.model.getLastPictureOpened(), Model.PICTURE_LARGE) != null)
                    mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_SHARE_PICTURE, mvc.model.getLastPictureOpened());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override @UiThread
    public void onModelChanged() {
        int lastPictureOpened = mvc.model.getLastPictureOpened();

        if(lastPictureOpened >= 0) {
            picture.setImageBitmap(mvc.model.getPictureFromCache(mvc.model.getLastPictureOpened(), Model.PICTURE_LARGE));
            comments.setAdapter(new CustomAdapter());
            comments_label.setText(String.format("%s: (%s)", getResources().getString(R.string.comments), comments.getAdapter().getCount()));
        }
    }

    private class CustomAdapter extends ArrayAdapter<Model.PictureInfo.Comment> {
        private Model.PictureInfo.Comment[] comments = mvc.model.getCommentsOfPictureInfoAtPosition(mvc.model.getLastPictureOpened());
        private ViewHolder viewHolder;

        private class ViewHolder {
            private TextView comment;
        }

        private CustomAdapter() {
            super(getActivity(), R.layout.fragment_picture_item, mvc.model.getCommentsOfPictureInfoAtPosition(mvc.model.getLastPictureOpened()));
        }

        @Override @UiThread @NonNull
        public android.view.View getView(int position, android.view.View convertView, @Nullable ViewGroup parent) {
            if(convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_picture_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.comment = (TextView) convertView.findViewById(R.id.comment);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            viewHolder.comment.setText(Html.fromHtml(comments[position].toFormattedString()));

            return convertView;
        }
    }

    private class CustomBroacastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String bitmapPath = (String) intent.getSerializableExtra(ExecutorIntentService.PARAM_BITMAP_PATH);
            Uri bitmapURI = Uri.fromFile(new File(bitmapPath));

            Intent sharePic = new Intent(Intent.ACTION_SEND);
            sharePic.setType("image/jpg");
            sharePic.putExtra(Intent.EXTRA_STREAM, bitmapURI);
            if(isAdded()) startActivity(Intent.createChooser(sharePic, getResources().getString(R.string.share_image_using)));
        }
    }
}
