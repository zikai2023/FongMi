package com.fongmi.android.tv.bean;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hot {
    @SerializedName("listInfo")
    private List<Data> data;

    private static Hot objectFrom(String str) {
        return App.gson().fromJson(str, Hot.class);
    }

    public static List<Hot.Data> get(String str) {
        try {
            return new ArrayList<>(objectFrom(str).getData());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Hot.Data> getData() {
        return data;
    }

    public static class Data {
        @SerializedName("title")
        private String name;

        @SerializedName("trend")
        private int trend = 2;

        @SerializedName("rankNum")
        private int rank = 0;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getRank() {
            return this.rank;
        }

        public int getTrend() {
            return this.trend;
        }

        @NonNull
        @Override
        public String toString() {
            return App.gson().toJson(this);
        }
    }

    public static int getTrendDrawable(int trend) {
        return switch (trend) {
            case -1 -> R.drawable.vector_drawable_hot_down;
            case 0 -> R.drawable.vector_drawable_hot_level;
            case 1 -> R.drawable.vector_drawable_hot_up;
            default -> R.drawable.vector_drawable_hot_blank;
        };
    }

    public static int getRankDrawable(int number) {
        return switch (number) {
            case 0 -> R.drawable.vector_drawable_number_0_small;
            case 1 -> R.drawable.vector_drawable_number_1_small;
            case 2 -> R.drawable.vector_drawable_number_2_small;
            case 3 -> R.drawable.vector_drawable_number_3_small;
            case 4 -> R.drawable.vector_drawable_number_4_small;
            case 5 -> R.drawable.vector_drawable_number_5_small;
            case 6 -> R.drawable.vector_drawable_number_6_small;
            case 7 -> R.drawable.vector_drawable_number_7_small;
            case 8 -> R.drawable.vector_drawable_number_8_small;
            case 9 -> R.drawable.vector_drawable_number_9_small;
            case 10 -> R.drawable.vector_drawable_number_10_small;
            case 11 -> R.drawable.vector_drawable_number_11_small;
            case 12 -> R.drawable.vector_drawable_number_12_small;
            case 13 -> R.drawable.vector_drawable_number_13_small;
            case 14 -> R.drawable.vector_drawable_number_14_small;
            case 15 -> R.drawable.vector_drawable_number_15_small;
            case 16 -> R.drawable.vector_drawable_number_16_small;
            case 17 -> R.drawable.vector_drawable_number_17_small;
            case 18 -> R.drawable.vector_drawable_number_18_small;
            case 19 -> R.drawable.vector_drawable_number_19_small;
            case 20 -> R.drawable.vector_drawable_number_20_small;
            default -> R.drawable.vector_drawable_number_more_small;
        };
    }
}
