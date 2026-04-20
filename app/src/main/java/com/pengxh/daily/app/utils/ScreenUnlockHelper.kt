package com.pengxh.daily.app.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.pengxh.daily.app.ui.UnlockActivity

/**
 * 屏幕解锁助手
 *
 * 启动透明 [UnlockActivity] 完成：唤醒屏幕 → 解除无密码锁屏 → 回桌面。
 * 不依赖 root，仅适用于 **无锁屏密码（None / Swipe）** 的场景。
 *
 * 若设备设置了 PIN/密码/图案，系统会拉起密码输入界面，无法静默解锁。
 */
object ScreenUnlockHelper {

    private const val kTag = "ScreenUnlockHelper"

    fun wakeUnlockAndGoHome(context: Context) {
        val intent = Intent(context, UnlockActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(kTag, "wakeUnlockAndGoHome: 启动 UnlockActivity 失败", e)
        }
    }
}
