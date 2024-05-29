package com.fongmi.android.tv.model;

import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.LiveParser;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.github.catvod.net.OkHttp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LiveViewModel extends ViewModel {

    private final static String TAG = LiveViewModel.class.getName();

    private static final int LIVE = 0;
    private static final int EPG = 1;
    private static final int URL = 2;

    private final SimpleDateFormat formatDate;
    private final SimpleDateFormat formatTime;

    public MutableLiveData<Channel> url;
    public MutableLiveData<Live> live;
    public MutableLiveData<Epg> epg;

    private ExecutorService executor1;
    private ExecutorService executor2;
    private ExecutorService executor3;

    public LiveViewModel() {
        this.formatTime = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault());
        this.formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.live = new MutableLiveData<>();
        this.epg = new MutableLiveData<>();
        this.url = new MutableLiveData<>();
    }

    public void getLive(Live item) {
        execute(LIVE, () -> {
            VodConfig.get().setRecent(item.getJar());
            LiveParser.start(item);
            verify(item);
            return item;
        });
    }

    public void getEpg(Channel item) {
        String date = formatDate.format(new Date());
        String url = item.getEpg().replace("{date}", date);
        Log.d(TAG, String.format(Locale.US, "getEpg: date=%s, url=%s", date, url));
        execute(EPG, () -> {
            Log.d(TAG, String.format(Locale.US, "Before: name=%s, catchup=%s, data=%s", item.getName(), item.getCatchup().getSource(), item.getData().getList()));
            if (!item.getData().equal(date)) item.setData(Epg.objectFrom(OkHttp.string(url), item.getName(), formatTime));
            Log.d(TAG, String.format(Locale.US, "After: name=%s, catchup=%s, data=%s", item.getName(), item.getCatchup().getSource(), item.getData().getList()));
            return item.getData().selected();
        });
    }

    public void getUrl(Channel item) {
        execute(URL, () -> {
            item.setMsg(null);
            Source.get().stop();
            item.setUrl(Source.get().fetch(item));
            return item;
        });
    }

    public void getUrl(Channel item, EpgData data) {
        execute(URL, () -> {
            item.setMsg(null);
            Source.get().stop();

            Log.d(TAG, String.format(Locale.US, "catchup: type=%s, source=%s", item.getCatchup().getType(), item.getCatchup().getSource()));
            //如果是#EXTINF: 上备注了 catchup = default ,则是替换整个url,实现了直播时用组播源,回看使用rtsp源
            if(item.getCatchup().getType().equals("default")){
                item.setUrl(item.getCatchup().format(data));
            }
            else{
                /**
                 * 更新项的URL。
                 * 此方法检查当前项的URL中是否包含问号，如果包含，会在URL末尾添加经过格式化的数据，
                 * 并且替换掉任何现有的"?playseek"为"&playseek"。如果URL中不包含问号，则直接在末尾添加格式化的数据。
                 *
                 * @param item 代表待更新URL的项
                 * @param data 用于格式化和追加到URL的数据
                 */
                if (item.getCurrent().indexOf('?') != -1)
                {
                    // 如果当前URL中包含问号，追加格式化后的数据到URL，并替换任何存在的"?playseek"为"&playseek"
                    item.setUrl(item.getCurrent().replace("PLTV","TVOD") + item.getCatchup().format(data).replace("?playseek","&playseek"));
                }
                else{
                    // 如果当前URL中不包含问号，直接追加格式化后的数据到URL
                    item.setUrl(item.getCurrent().replace("PLTV","TVOD") + item.getCatchup().format(data).replace("&playseek","?playseek"));
                }

            }
            Log.d(TAG, String.format(Locale.US, "catchup after: url=%s", item.getUrl()));

            return item;
        });
    }

    private void verify(Live item) {
        Iterator<Group> iterator = item.getGroups().iterator();
        while (iterator.hasNext()) if (iterator.next().isEmpty()) iterator.remove();
    }

    private void execute(int type, Callable<?> callable) {
        switch (type) {
            case LIVE:
                if (executor1 != null) executor1.shutdownNow();
                executor1 = Executors.newFixedThreadPool(2);
                executor1.execute(runnable(type, callable, executor1));
                break;
            case EPG:
                if (executor2 != null) executor2.shutdownNow();
                executor2 = Executors.newFixedThreadPool(2);
                executor2.execute(runnable(type, callable, executor2));
                break;
            case URL:
                if (executor3 != null) executor3.shutdownNow();
                executor3 = Executors.newFixedThreadPool(2);
                executor3.execute(runnable(type, callable, executor3));
                break;
        }
    }

    private Runnable runnable(int type, Callable<?> callable, ExecutorService executor) {
        return () -> {
            try {
                if (Thread.interrupted()) return;
                if (type == EPG) epg.postValue((Epg) executor.submit(callable).get(Constant.TIMEOUT_EPG, TimeUnit.MILLISECONDS));
                if (type == LIVE) live.postValue((Live) executor.submit(callable).get(Constant.TIMEOUT_LIVE, TimeUnit.MILLISECONDS));
                if (type == URL) url.postValue((Channel) executor.submit(callable).get(Constant.TIMEOUT_PARSE_LIVE, TimeUnit.MILLISECONDS));
            } catch (Throwable e) {
                if (e instanceof InterruptedException || Thread.interrupted()) return;
                if (e.getCause() instanceof ExtractException) url.postValue(Channel.error(e.getCause().getMessage()));
                else if (type == URL) url.postValue(new Channel());
                if (type == LIVE) live.postValue(new Live());
                if (type == EPG) epg.postValue(new Epg());
                e.printStackTrace();
            }
        };
    }

    @Override
    protected void onCleared() {
        if (executor1 != null) executor1.shutdownNow();
        if (executor2 != null) executor2.shutdownNow();
        if (executor3 != null) executor3.shutdownNow();
    }
}
