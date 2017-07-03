package it.univr.francesco.flickr.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;
import it.univr.francesco.flickr.controller.ExecutorIntentService;
import it.univr.francesco.flickr.model.Model;

public class ListFragment extends android.app.ListFragment implements AbstractFragment {
    private final static String TAG = ListFragment.class.getName();
    private MVC mvc;

    private CustomBroacastReceiver customBroacastReceiver;
    private IntentFilter intentFilter;

    @Override @UiThread
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        customBroacastReceiver = new CustomBroacastReceiver();
        intentFilter = new IntentFilter(ExecutorIntentService.ACTION_SEND_BITMAP_PATH);

        return view;
    }

    @Override @UiThread
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mvc = ((Flickr) getActivity().getApplication()).getMVC();

        getView().setBackgroundColor(Color.WHITE);
        getView().setClickable(true);

        getListView().setOnItemClickListener((parent, view, position, id) -> {
            if(mvc.model.getPictureInfoAtPosition(position) != null) {
                mvc.controller.setLastPictureOpened(position);
                mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_COMMENTS, position);
                if(mvc.model.getPictureFromCache(position, Model.PICTURE_LARGE) == null) {
                    mvc.controller.addPictureToCache(position, BitmapFactory.decodeResource(getResources(), R.drawable.empty), Model.PICTURE_LARGE);
                    mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_PICTURE, position);
                }
                mvc.controller.showPicture();
            }
        });

        setListAdapter(new CustomAdapter());
        onModelChanged();
    }

    @Override @UiThread
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(customBroacastReceiver, intentFilter);
        registerForContextMenu(getListView());
    }

    @Override @UiThread
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(customBroacastReceiver);
        unregisterForContextMenu(getListView());
    }

    @Override @UiThread
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        getActivity().getMenuInflater().inflate(R.menu.menu_context, menu);
    }

    @Override @UiThread
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);

        int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;

        switch (item.getItemId()) {
            case R.id.context_menu_share_item:
                if(mvc.model.getPictureInfoAtPosition(position) != null)
                    mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_SHARE_PICTURE, position);
                break;
            case R.id.context_menu_visit_author:
                if(mvc.model.getPictureInfoAtPosition(position) != null) {
                    mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_AUTHOR_INFO_GENERAL, position);
                    mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_RECENT_UPLOADS_URLS, position);
                    mvc.controller.showAuthor();
                }
                break;
        }

        return true;
    }

    @Override @UiThread
    public void onModelChanged() {
        ((CustomAdapter) getListAdapter()).notifyDataSetChanged();
    }

    private class CustomAdapter extends ArrayAdapter<Model.PictureInfo> {
        private Model.PictureInfo pictureInfo;
        private ViewHolder viewHolder;

        private class ViewHolder {
            private ImageView preview;
            private TextView caption;
        }

        private CustomAdapter() {
            super(getActivity(), R.layout.fragment_list_item, mvc.model.getPictureInfos());
        }

        @Override @UiThread @NonNull
        public View getView(int position, View convertView, @Nullable ViewGroup parent) {
            pictureInfo = mvc.model.getPictureInfoAtPosition(position);

            if(convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.preview = (ImageView) convertView.findViewById(R.id.preview);
                viewHolder.caption = (TextView) convertView.findViewById(R.id.caption);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            if (pictureInfo == null)
                return convertView;

            if (mvc.model.getPictureFromCache(position, Model.PICTURE_SMALL) == null) {
                mvc.controller.addPictureToCache(position, BitmapFactory.decodeResource(getResources(), R.drawable.empty), Model.PICTURE_SMALL);
                mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_PREVIEW, position);
            }

            viewHolder.preview.setImageBitmap(mvc.model.getPictureFromCache(position, Model.PICTURE_SMALL));
            viewHolder.caption.setText(Html.fromHtml(pictureInfo.toFormattedString()));

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