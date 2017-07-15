package it.univr.francesco.flickr.view;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
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
import android.view.Window;
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

import static it.univr.francesco.flickr.model.Model.PICTURE_LARGE;

public class PictureFragment extends android.app.Fragment implements AbstractFragment {
    private MVC mvc;

    public final static String LAST_PICTURE_OPENED = "lastPictureOpened";
    private int lastPictureOpened;

    private ImageView picture;
    private TextView comments_label;
    private ListView comments;

    private CustomBroacastReceiver customBroacastReceiver;
    private IntentFilter intentFilter;

    @Override @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        lastPictureOpened = getArguments().getInt(LAST_PICTURE_OPENED);
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

        picture.setOnClickListener(v -> showPictureDialog());

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
                // Share picture large only if it has been downloaded
                Bitmap bitmap = mvc.model.getPictureInfo(lastPictureOpened).getPicture(PICTURE_LARGE);
                if (!bitmap.sameAs(BitmapFactory.decodeResource(getResources(), R.drawable.empty)))
                    mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_SHARE_PICTURE, lastPictureOpened, true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override @UiThread
    public void onModelChanged() {
        if(mvc.model.getPictureInfos().length != 0) {
            picture.setImageBitmap(mvc.model.getPictureInfo(lastPictureOpened).getPicture(PICTURE_LARGE));
            comments.setAdapter(new CustomAdapter());
            comments_label.setText(String.format("%s: (%s)", getResources().getString(R.string.comments), comments.getAdapter().getCount()));
        }
    }

    @UiThread
    private void showPictureDialog() {
        // If the picture large is not downloaded, return
        Bitmap bitmap = mvc.model.getPictureInfo(lastPictureOpened).getPicture(PICTURE_LARGE);
        if (bitmap.sameAs(BitmapFactory.decodeResource(getResources(), R.drawable.empty))) return;

        // The picture large is ready to be shown
        Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_picture);

        ImageView dialog_picture = (ImageView) dialog.findViewById(R.id.dialog_picture);
        dialog_picture.setImageBitmap(bitmap);
        dialog_picture.setOnClickListener(v1 -> dialog.dismiss());

        dialog.show();
    }

    private class CustomAdapter extends ArrayAdapter<String> {
        private String[] comments = mvc.model.getPictureInfo(lastPictureOpened).getComments();
        private ViewHolder viewHolder;

        private class ViewHolder {
            private TextView comment;
        }

        private CustomAdapter() {
            super(getActivity(), R.layout.fragment_picture_item, mvc.model.getPictureInfo(lastPictureOpened).getComments());
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

            viewHolder.comment.setText(Html.fromHtml(comments[position]));

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
            if(isAdded()) startActivity(Intent.createChooser(sharePic, getResources().getString(R.string.share_image_using) + ":"));
        }
    }
}
