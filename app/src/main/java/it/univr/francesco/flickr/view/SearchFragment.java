package it.univr.francesco.flickr.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import it.univr.francesco.flickr.BuildConfig;
import it.univr.francesco.flickr.Flickr;
import it.univr.francesco.flickr.MVC;
import it.univr.francesco.flickr.R;
import it.univr.francesco.flickr.Utils;
import it.univr.francesco.flickr.controller.ExecutorIntentService;

public class SearchFragment extends Fragment implements AbstractFragment {
    private MVC mvc;
    
    private EditText stringToSearch;
    private Button searchButton;
    private Button recentButton;
    private Button popularButton;

    @Override @UiThread
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override @UiThread
    public android.view.View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(R.layout.fragment_search, container, false);
        getActivity().setTitle("Flickr");

        mvc = ((Flickr) getActivity().getApplication()).getMVC();

        stringToSearch = (EditText) view.findViewById(R.id.string_to_search);
        searchButton = (Button) view.findViewById(R.id.search_button);
        recentButton = (Button) view.findViewById(R.id.recent_button);
        popularButton = (Button) view.findViewById(R.id.popular_button);

        return view;
    }

    @Override @UiThread
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        searchButton.setOnClickListener(v -> {
            String str = stringToSearch.getText().toString();
            if(!str.isEmpty()) {
                Utils.hideKeyboard(getActivity());

                int searchID = mvc.model.searchID.incrementAndGet();
                mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_PICTURE_INFOS, str, 0, searchID);
                mvc.controller.showList();
                getActivity().setTitle(str);
            }
        });

        recentButton.setOnClickListener(v -> {
            Utils.hideKeyboard(getActivity());

            int searchID = mvc.model.searchID.incrementAndGet();
            mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_PICTURE_INFOS, null, 1, searchID);
            mvc.controller.showList();
            getActivity().setTitle(getResources().getString(R.string.recent));
        });

        popularButton.setOnClickListener(v -> {
            Utils.hideKeyboard(getActivity());

            int searchID = mvc.model.searchID.incrementAndGet();
            mvc.controller.startService(getActivity(), ExecutorIntentService.ACTION_GET_PICTURE_INFOS, null, 2, searchID);
            mvc.controller.showList();
            getActivity().setTitle(getResources().getString(R.string.popular));
        });

        onModelChanged();
    }

    @Override @UiThread
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override @UiThread
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_show_infos:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setTitle(Html.fromHtml(String.format("<b>%s:</b>", getResources().getString(R.string.about))));

                builder.setMessage(Html.fromHtml(String.format("<b>%s:</b> %s<br><b>%s:</b> %s<br><b>%s:</b> %s",
                        getResources().getString(R.string.author), getResources().getString(R.string.ft),
                        getResources().getString(R.string.version), BuildConfig.VERSION_NAME,
                        getResources().getString(R.string.date), new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.ITALY).format(new Date()))));

                builder.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override @UiThread
    public void onModelChanged() {
        // Nothing to do
    }
}
