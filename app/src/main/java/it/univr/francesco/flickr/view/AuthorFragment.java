package it.univr.francesco.flickr.view;

import android.app.Fragment;
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
import it.univr.francesco.flickr.ImageManager;
import it.univr.francesco.flickr.model.Model;


public class AuthorFragment extends Fragment implements AbstractFragment {
    private MVC mvc;

    public final static String AUTHOR_ID = "authorID";
    private String authorID;

    private ImageView profile_picture;
    private TextView author_username;
    private TextView author_realname;
    private TextView author_location;
    private TextView author_description;
    private GridView gridView;

    @Override @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authorID = getArguments().getString(AUTHOR_ID);
    }

    @Override @UiThread
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_author, container, false);

        mvc = ((Flickr) getActivity().getApplication()).getMVC();

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

        onModelChanged();
    }

    @Override @UiThread
    public void onModelChanged() {
        Model.AuthorInfo authorInfo = mvc.model.getAuthorInfo(authorID);

        // Update only once
        if(author_username.getText().toString().isEmpty() && !authorInfo.profile_image_url.isEmpty()) {
            ImageManager.display(authorInfo.profile_image_url, profile_picture);
            author_username.setText(authorInfo.username);
            author_realname.setText(Html.fromHtml(String.format("<font color='black'>%s: </font>%s", getResources().getString(R.string.real_name), authorInfo.realname)));
            author_location.setText(Html.fromHtml(String.format("<font color='black'>%s: </font>%s", getResources().getString(R.string.location), authorInfo.location)));
            author_description.setText(Html.fromHtml(String.format("<font color='black'>%s: </font>%s", getResources().getString(R.string.description), authorInfo.description)));
        }

        gridView.setAdapter(new CustomAdapter());
    }

    private class CustomAdapter extends ArrayAdapter<String> {
        private final LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        private ViewHolder viewHolder;

        private class ViewHolder {
            private ImageView gridImage;
        }

        private CustomAdapter() {
            super(getActivity(), R.layout.fragment_grid_item, mvc.model.getAuthorInfo(authorID).getUrls());
        }

        @Override @UiThread @NonNull
        public View getView(int position, View convertView, @Nullable ViewGroup parent) {
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.fragment_grid_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.gridImage = (ImageView) convertView.findViewById(R.id.gridImage);
                convertView.setTag(viewHolder);
            } else
                viewHolder = (ViewHolder) convertView.getTag();

            String recentUploadsURL = getItem(position);
            if(recentUploadsURL == null) return convertView;

            ImageManager.display(recentUploadsURL, viewHolder.gridImage);

            return convertView;
        }
    }
}
