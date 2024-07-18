package com.fongmi.android.tv.bean;

import android.util.Log;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.db.AppDatabase;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fongmi.android.tv.db.dao.HistoryDao;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.TimeZone;
import java.util.Calendar;

public class HistorySyncManager {

    private static final String TAG = "HistorySyncManager";
    private static final String GIST_TOKEN = "xxxajfhfiejfbf";
    private static final String GIST_URL = "https://api.github.com/gists/YOUR_GIST_ID";
    private FtpManager ftpManager;

    public HistorySyncManager(FtpManager ftpManager) {
        this.ftpManager = ftpManager;
    }

//    public HistorySyncManager() {
//        ftpManager = new FtpManager("192.168.1.1", 21, "hoanayang", "ilovebob123", false);
//    }

    public HistorySyncManager(String uri, String username, String password) {
        ftpManager = new FtpManager(uri, username, password);
    }

    public void syncAll() {
        String jsonData;
        try {
            if (!ftpManager.isServerReachable)
            {
                return;
            }

            Gson gson =  new Gson();
            jsonData = ftpManager.downloadJsonFileAsString(null);///USB2T/(Documents)/(TVBox)/TV2/TV.json");
            JsonObject jsonObject = jsonData==null? new JsonObject(): gson.fromJson(jsonData, JsonObject.class);
            List<History> ftpItems = jsonData==null? new ArrayList<>(): parseHistoryList(jsonData);
            List<History> sqliteItems =AppDatabase.get().getHistoryDao().getAll();
            List<History> newMergedItems = History.syncLists(sqliteItems, ftpItems);
            AppDatabase.get().runInTransaction(new Runnable() {
                @Override
                public void run() {
                    AppDatabase.get().getHistoryDao().insertOrUpdate(newMergedItems);
                    //TODO or NotToDo: delete hard, if lastupdated is more than xx days old, delete it from the database
                    //AppDatabase.get().getHistoryDao().deleteHard();
                }
            });

            jsonObject.add("History", gson.toJsonTree(newMergedItems));
            String updatedJsonString = gson.toJson(jsonObject);
            //ftpManager.uploadJsonString(updatedJsonString, "/USB2T/(Documents)/(TVBox)/TV2/TV.json");
            ftpManager.uploadJsonString(updatedJsonString, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        public List<History> parseHistoryList(String jsonString) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(History.class, new JsonDeserializer<History>() {
                        @Override
                        public History deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                            JsonObject jsonObject = json.getAsJsonObject();
                            History history = new History();

                            history.setKey(jsonObject.get("key").getAsString());
                            history.setVodPic(jsonObject.has("vodPic") ? jsonObject.get("vodPic").getAsString() : null);
                            history.setVodName(jsonObject.has("vodName") ? jsonObject.get("vodName").getAsString() : null);
                            history.setVodFlag(jsonObject.has("vodFlag") ? jsonObject.get("vodFlag").getAsString() : null);
                            history.setVodRemarks(jsonObject.has("vodRemarks") ? jsonObject.get("vodRemarks").getAsString() : null);
                            history.setEpisodeUrl(jsonObject.has("episodeUrl") ? jsonObject.get("episodeUrl").getAsString() : null);
                            history.setRevSort(jsonObject.has("revSort") && jsonObject.get("revSort").getAsBoolean());
                            history.setRevPlay(jsonObject.has("revPlay") && jsonObject.get("revPlay").getAsBoolean());
                            history.setCreateTime(jsonObject.has("createTime") ? jsonObject.get("createTime").getAsLong() : 0L);
                            history.setOpening(jsonObject.has("opening") ? jsonObject.get("opening").getAsLong() : 0L);
                            history.setEnding(jsonObject.has("ending") ? jsonObject.get("ending").getAsLong() : 0L);
                            history.setPosition(jsonObject.has("position") ? jsonObject.get("position").getAsLong() : 0L);
                            history.setDuration(jsonObject.has("duration") ? jsonObject.get("duration").getAsLong() : 0L);
                            history.setSpeed(jsonObject.has("speed") ? jsonObject.get("speed").getAsFloat() : 1.0f);
                            history.setPlayer(jsonObject.has("player") ? jsonObject.get("player").getAsInt() : 0);
                            history.setScale(jsonObject.has("scale") ? jsonObject.get("scale").getAsInt() : 0);
                            history.setCid(jsonObject.has("cid") ? jsonObject.get("cid").getAsInt() : -1);

                            // Set default values for missing fields
                            history.setLastUpdated(jsonObject.has("lastUpdated") ? jsonObject.get("lastUpdated").getAsLong() : getCurrentUTCTime());
                            history.setDeleted(jsonObject.has("deleted") && jsonObject.get("deleted").getAsBoolean());

                            return history;
                        }
                    })
                    .create();

            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            String historyArrayString = jsonObject.getAsJsonArray("History").toString();
            Type listType = new TypeToken<List<History>>(){}.getType();
            return gson.fromJson(historyArrayString, listType);
        }

    private static long getCurrentUTCTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return calendar.getTimeInMillis();
    }

    private void updateSQLite(List<History> items) {
        AppDatabase.get().getHistoryDao().insertOrUpdate(items);
    }

    private List<History> getItemsFromGist() {
        List<History> items = new ArrayList<>();
        try {
            URL url = new URL(GIST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "token " + GIST_TOKEN);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            JSONObject jsonObject = new JSONObject(response.toString());
            JSONObject files = jsonObject.getJSONObject("files");
            JSONObject tvJson = files.getJSONObject("tv.json");
            String content = tvJson.getString("content");

            items = History.arrayFrom(content);

            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching data from Gist", e);
        }
        return items;
    }



    private void updateGist(List<History> items) {
        try {
            JSONObject contentJson = new JSONObject();
            JSONArray historyArray = new JSONArray(App.gson().toJson(items));
            contentJson.put("History", historyArray);

            JSONObject gistContent = new JSONObject();
            gistContent.put("content", contentJson.toString());

            JSONObject files = new JSONObject();
            files.put("tv.json", gistContent);

            JSONObject requestBody = new JSONObject();
            requestBody.put("files", files);

            URL url = new URL(GIST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Authorization", "token " + GIST_TOKEN);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error updating Gist", e);
        }
    }


}