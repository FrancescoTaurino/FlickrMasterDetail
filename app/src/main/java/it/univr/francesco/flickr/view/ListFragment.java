package it.univr.francesco.flickr.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;
import it.univr.francesco.flickr.Utils;
import it.univr.francesco.flickr.controller.ExecutorIntentService;
import it.univr.francesco.flickr.ImageManager;
import it.univr.francesco.flickr.model.Model;

import static it.univr.francesco.flickr.ImageManager.ACTION_SEND_BITMAP_PATH;

public class ListFragment extends android.app.ListFragment implements AbstractFragment {
    private MVC mvc;

    private CustomBroacastReceiver customBroacastReceiver;
    private IntentFilter intentFilter;

    @Override @UiThread
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mvc = ((Flickr) getActivity().getApplication()).getMVC();

        customBroacastReceiver = new CustomBroacastReceiver();
        intentFilter = new IntentFilter(ACTION_SEND_BITMAP_PATH);

        return view;
    }

    @Override @UiThread
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener((parent, view, position, id) -> {
            Model.PictureInfo pictureInfo = ((CustomAdapter) getListAdapter()).getItem(position);
            String pictureID = pictureInfo == null ? "" : pictureInfo.pictureID;

            mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_COMMENTS, pictureID);
            mvc.controller.showPicture(pictureID);
        });

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
        Model.PictureInfo pictureInfo = ((CustomAdapter) getListAdapter()).getItem(position);

        switch (item.getItemId()) {
            case R.id.context_menu_share_item:
                if(pictureInfo != null) ImageManager.share(getActivity(), pictureInfo.pictureURL, pictureInfo.pictureID);

                break;
            case R.id.context_menu_visit_author:
                String authorID = pictureInfo == null ? "" : pictureInfo.authorID;

                mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_AUTHOR_INFOS, authorID);
                mvc.controller.showAuthor(authorID);
                break;
        }

        return true;
    }

    @Override @UiThread
    public void onModelChanged() {
        setListAdapter(new CustomAdapter());
    }

    private class CustomAdapter extends ArrayAdapter<Model.PictureInfo> {
        private final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
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
            if(convertView == null) {
                convertView = layoutInflater.inflate(R.layout.fragment_list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.preview = (ImageView) convertView.findViewById(R.id.preview);
                viewHolder.caption = (TextView) convertView.findViewById(R.id.caption);
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();

            Model.PictureInfo pictureInfo = getItem(position);
            if(pictureInfo == null) return convertView;

            ImageManager.display(pictureInfo.previewURL, viewHolder.preview);
            viewHolder.caption.setText(Html.fromHtml(pictureInfo.caption));

            return convertView;
        }
    }

    private class CustomBroacastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isAdded())
                startActivity(Intent.createChooser(Utils.getIntentToShare(intent), getResources().getString(R.string.share_image_using)));
        }
    }
}
