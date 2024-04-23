package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyboard;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class SearchActivity extends BaseActivity implements WordAdapter.OnClickListener, RecordAdapter.OnClickListener, CustomKeyboard.Callback, SiteCallback {

    private ActivitySearchBinding mBinding;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SearchActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        CustomKeyboard.init(this, mBinding);
        setRecyclerView();
        getHot();
    }

    @Override
    protected void initEvent() {
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onSearch();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
        mBinding.mic.setListener(this, new CustomTextListener() {
            @Override
            public void onEndOfSpeech() {
                mBinding.keyword.requestFocus();
                mBinding.mic.stop();
            }

            @Override
            public void onResults(String result) {
                mBinding.keyword.setText(result);
                mBinding.keyword.setSelection(mBinding.keyword.length());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.wordRecycler.setHasFixedSize(true);
        mBinding.wordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.recordRecycler.setHasFixedSize(true);
        mBinding.recordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
    }

    private void getHot() {
        mBinding.hint.setText(R.string.search_hot);
        mWordAdapter.addAll(Hot.get(Setting.getHot()));

        OkHttp.newCall("https://node.video.qq.com/x/api/hot_search/?callback=&channelId=0&otype=json").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String itemList = JsonParser.parseString(response.body().string()).getAsJsonObject()
                        .get("data").getAsJsonObject()
                        .get("mapResult").getAsJsonObject()
                        .get("0").getAsJsonObject().toString();
                Setting.putHot(itemList);
                if (mWordAdapter.getItemCount() > 0) return;
                App.post(() -> mWordAdapter.addAll(Hot.get(itemList)));
            }
        });
    }

    private void getSuggest(String text) {
        mBinding.hint.setText(R.string.search_suggest);
        mWordAdapter.clear();
        OkHttp.newCall("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box?format=json&page_num=0&page_size=10&key=" + text).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (mBinding.keyword.getText().toString().trim().isEmpty()) return;
                List<Hot.Data> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.appendAll(items));
            }
        });
    }

    @Override
    public void onItemClick(Hot.Data text) {
        mBinding.keyword.setText(text.getName());
        onSearch();
    }

    @Override
    public void onItemClick(String text) {
        mBinding.keyword.setText(text);
        onSearch();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.recordLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSearch() {
        String keyword = mBinding.keyword.getText().toString().trim();
        mBinding.keyword.setSelection(mBinding.keyword.length());
        Util.hideKeyboard(mBinding.keyword);
        if (TextUtils.isEmpty(keyword)) return;
        CollectActivity.start(this, keyword);
        App.post(() -> mRecordAdapter.add(keyword), 250);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event)) showDialog();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).search().show();
    }

    @Override
    public void onRemote() {
        PushActivity.start(this, 1);
    }

    @Override
    public void setSite(Site item) {
    }

    @Override
    public void onChanged() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.keyword.requestFocus();
    }
}
