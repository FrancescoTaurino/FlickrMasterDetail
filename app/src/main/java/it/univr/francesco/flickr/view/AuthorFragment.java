package it.univr.francesco.flickr.view;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;
import it.univr.francesco.flickr.controller.ExecutorIntentService;
import it.univr.francesco.flickr.model.Model;


public class AuthorFragment extends Fragment implements AbstractFragment {
    private MVC mvc;

    private ImageView profile_picture;
    private TextView author_username;
    private TextView author_realname;
    private TextView author_location;
    private TextView author_description;
    private GridView gridView;


    @Override @UiThread
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_author, container, false);

        profile_picture = (ImageView) view.findViewById(R.id.profile_picture);
        author_username = (TextView) view.findViewById(R.id.author_username);
        author_realname = (TextView) view.findViewById(R.id.author_realname);
        author_location = (TextView) view.findViewById(R.id.author_location);
        author_description = (TextView) view.findViewById(R.id.author_description);
        gridView = (GridView) view.findViewById(R.id.gridView);

        return view;
    }

    @Override @UiThread
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mvc = ((Flickr) getActivity().getApplication()).getMVC();

        gridView.setAdapter(new CustomAdapter());
        onModelChanged();
    }

    @Override @UiThread
    public void onModelChanged() {
        Model.AuthorInfo.AuthorInfoGeneral authorInfoGeneral = mvc.model.getAuthorInfoGeneral();

        if(authorInfoGeneral != null && author_username.getText().toString().isEmpty()) {
            profile_picture.setImageBitmap(authorInfoGeneral.profile_image);
            author_username.setText(authorInfoGeneral.username);
            author_realname.setText(Html.fromHtml(String.format("<font color='black'>%s: </font>%s", getResources().getString(R.string.real_name), authorInfoGeneral.realname)));
            author_location.setText(Html.fromHtml(String.format("<font color='black'>%s: </font>%s", getResources().getString(R.string.location), authorInfoGeneral.location)));
            author_description.setText(Html.fromHtml(String.format("<font color='black'>%s: </font>%s", getResources().getString(R.string.description), authorInfoGeneral.description)));
        }

        ((CustomAdapter) gridView.getAdapter()).notifyDataSetChanged();
    }

    private class CustomAdapter extends ArrayAdapter<String> {
        private String recentUploadURL;
        private ViewHolder viewHolder;

        private class ViewHolder {
            private ImageView gridImage;
        }

        private CustomAdapter() {
            super(getActivity(), R.layout.fragment_grid_item, mvc.model.getURLsFromRecentUploads());
        }

        @Override @UiThread @NonNull
        public View getView(int position, View convertView, @Nullable ViewGroup parent) {
            recentUploadURL = (String) mvc.model.getFromRecentUploadsAtPosition(position, Model.URL);

            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_grid_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.gridImage = (ImageView) convertView.findViewById(R.id.gridImage);
                convertView.setTag(viewHolder);
            } else
                viewHolder = (ViewHolder) convertView.getTag();

            if (recentUploadURL == null)
                return convertView;

            if (mvc.model.getFromRecentUploadsAtPosition(position, Model.PIC) == null) {
                mvc.controller.addPicToRecentUploads(position, BitmapFactory.decodeResource(getResources(), R.drawable.empty));
                mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_RECENT_UPLOADS_PIC, position);
            }

            viewHolder.gridImage.setImageBitmap((Bitmap) mvc.model.getFromRecentUploadsAtPosition(position, Model.PIC));

            return convertView;
        }
    }
}
