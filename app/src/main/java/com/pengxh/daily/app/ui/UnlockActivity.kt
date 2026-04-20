package com.pengxh.daily.app.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager

/**
 * 透明 Activity：唤醒屏幕 → 解除无密码锁屏 → 回桌面 → 自动关闭
 *
 * 由 NotificationMonitorService 收到「开屏」指令后启动。
 * 不依赖 root；仅依赖普通权限 WAKE_LOCK / DISABLE_KEYGUARD / TURN_SCREEN_ON。
 *
 * 注意：本应用持有 SYSTEM_ALERT_WINDOW，因此满足 Android 10+ 后台启动 Activity 的豁免条件。
 */
class UnlockActivity : Activity() {

    private val kTag = "UnlockActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 显示在锁屏之上，且自动唤醒屏幕
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        // 兜底：保持屏幕常亮一小段时间，避免 dismiss 过程中再次熄屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        acquireWakeLock()
        dismissKeyguardThenGoHome()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE,
            "DailyTask:UnlockWakeLock"
        )
        // 持有 3 秒后系统自动释放，足够完成 dismiss + 回桌面
        wakeLock.acquire(3000L)
    }

    private fun dismissKeyguardThenGoHome() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardLocked) {
            // 屏幕未锁，直接回桌面
            goHomeAndFinish()
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                Log.d(kTag, "dismissKeyguard: 成功")
                goHomeAndFinish()
            }

            override fun onDismissError() {
                Log.w(kTag, "dismissKeyguard: 失败（可能设置了密码/PIN/图案）")
                goHomeAndFinish()
            }

            override fun onDismissCancelled() {
                Log.w(kTag, "dismissKeyguard: 取消")
                goHomeAndFinish()
            }
        })
    }

    private fun goHomeAndFinish() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(home)
        } catch (e: Exception) {
            Log.e(kTag, "goHome: 启动 Launcher 失败", e)
        }
        finish()
    }
}
