package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.App;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Suggest {

    @SerializedName("data")
    private Suggest.Data data;

    private static Suggest objectFrom(String str) {
        return App.gson().fromJson(str, Suggest.class);
    }

    public static List<Hot.Data> get(String str) {
        try {
            List<Hot.Data> items = new ArrayList<>();
            for (Suggest.GroupData item : objectFrom(str).getGroupData()) {
                Hot.Data map_list = new Hot.Data();
                map_list.setName(item.getAction().getActionArgs().getSearchKeyword().getStrVal());
                items.add(map_list);
            }
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Suggest.GroupData> getGroupData() {
        return data.getSearchData().getVecGroupData().get(0).getGroupData();
    }

    static class Data {

        @SerializedName("search_data")
        private SearchData searchData;

        private SearchData getSearchData() {
            return searchData;
        }
    }

    static class SearchData {

        @SerializedName("vecGroupData")
        private List<VecGroupData> vecGroupData;

        private List<VecGroupData> getVecGroupData() {
            return vecGroupData;
        }
    }

    static class VecGroupData {

        @SerializedName("group_data")
        private List<GroupData> groupData;

        private List<GroupData> getGroupData() {
            return groupData;
        }
    }

    static class GroupData {

        @SerializedName("action")
        private Action action;

        private Action getAction() {
            return action;
        }
    }

    static class Action {

        @SerializedName("actionArgs")
        private ActionArgs actionArgs;

        private ActionArgs getActionArgs() {
            return actionArgs;
        }
    }

    static class ActionArgs {

        @SerializedName("search_keyword")
        private SearchKeyword searchKeyword;

        private SearchKeyword getSearchKeyword() {
            return searchKeyword;
        }
    }

    static class SearchKeyword {

        @SerializedName("strVal")
        private String strVal;

        private String getStrVal() {
            return strVal;
        }
    }


}
