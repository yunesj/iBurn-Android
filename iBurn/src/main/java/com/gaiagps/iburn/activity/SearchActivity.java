package com.gaiagps.iburn.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.gaiagps.iburn.IntentUtil;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterListener;
import com.gaiagps.iburn.adapters.DividerItemDecoration;
import com.gaiagps.iburn.adapters.MultiTypePlayaItemAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItem;
import com.gaiagps.iburn.databinding.ActivitySearchBinding;
import com.tonicartos.superslim.LayoutManager;

import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SearchActivity extends AppCompatActivity implements AdapterListener {

    private MultiTypePlayaItemAdapter adapter;
    private Disposable searchSubscription;

    private ActivitySearchBinding binding;

    /*
    @BindView(R.id.results)
    RecyclerView resultList;

    @BindView(R.id.search)
    EditText searchEntry;

    @BindView(R.id.results_summary)
    TextView resultsSummary;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new MultiTypePlayaItemAdapter(this, this);

        RecyclerView resultList = binding.results;
        resultList.setLayoutManager(new LayoutManager(this));
        resultList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        resultList.setAdapter(adapter);

        binding.search.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.search, InputMethodManager.SHOW_IMPLICIT);

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dispatchSearchQuery(s.toString());
            }
        });

        binding.search.setOnEditorActionListener((view, actionId, event) -> {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(binding.search.getWindowToken(), 0);
            return true;
        });

    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        if (searchSubscription != null && !searchSubscription.isDisposed())
            searchSubscription.dispose();

        searchSubscription = DataProvider.Companion.getInstance(getApplicationContext())
                .flatMap(dataProvider -> dataProvider.observeNameQuery(query).toObservable()) // TODO : rm toObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sectionedPlayaItems -> {
                    binding.resultsSummary.setText(describeResults(sectionedPlayaItems));
                    adapter.setSectionedItems(sectionedPlayaItems);
                });

    }

    private String describeResults(DataProvider.SectionedPlayaItems searchResults) {
        return String.format(Locale.US, "%d results",
                searchResults.getData().size());
    }

    @Override
    public void onItemSelected(PlayaItem item) {
        IntentUtil.viewItemDetail(this, item);
    }

    @Override
    public void onItemFavoriteButtonSelected(PlayaItem item) {
        DataProvider.Companion.getInstance(getApplicationContext())
                .observeOn(Schedulers.io())
                .subscribe(dataProvider -> {
                    dataProvider.toggleFavorite(item);
                }, throwable -> Timber.e(throwable, "failed to toggle favorite"));
    }

    public void onBackButtonClick(View view) {
        this.finish();
    }
}
