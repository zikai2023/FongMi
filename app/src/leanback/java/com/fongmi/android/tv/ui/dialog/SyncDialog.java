package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogSyncBinding;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.SyncCallback;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SyncDialog{ // implements DialogInterface.OnDismissListener {

    private final DialogSyncBinding binding;
    private final FragmentActivity activity;
    private final AlertDialog dialog;

    public static SyncDialog create(FragmentActivity activity) {
        return new SyncDialog(activity);
    }

    public SyncDialog(FragmentActivity activity) {
        this.activity = activity;
        this.binding = DialogSyncBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
   }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.90f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.ftpServer.setText(Setting.getFtpUri());
        binding.ftpUsername.setText(Setting.getFtpUsername());
        binding.ftpPassword.setText(Setting.getFtpPassword());
    }

    private void initEvent() {
        EventBus.getDefault().register(this);
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
    }


    private void onPositive(View view) {
        Setting.putFtpPassword(binding.ftpPassword.getText().toString().trim());
        Setting.putFtpUsername(binding.ftpUsername.getText().toString().trim());
        Setting.putFtpUri(binding.ftpServer.getText().toString().trim());
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.SETTING) return;
    }


}
