package com.callcheck.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 재부팅 후 서비스를 자동으로 다시 시작한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NtfyService.start(context)
        }
    }
}
