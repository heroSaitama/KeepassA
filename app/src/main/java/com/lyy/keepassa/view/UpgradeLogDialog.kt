/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.core.content.edit
import com.arialyy.frame.base.BaseDialog
import com.arialyy.frame.util.AndroidUtils
import com.lyy.keepassa.R
import com.lyy.keepassa.base.Constance
import com.lyy.keepassa.databinding.DialogUpgradeBinding
import com.lyy.keepassa.util.FingerprintUtil
import com.lyy.keepassa.util.LanguageUtil
import com.lyy.keepassa.view.dialog.WebDavLoginDialog
import com.lyy.keepassa.view.fingerprint.FingerprintActivity
import com.lyy.keepassa.widget.toPx
import com.zzhoujay.richtext.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 版本升级对话框
 */
class UpgradeLogDialog : BaseDialog<DialogUpgradeBinding>() {
  val scope = MainScope()

  override fun setLayoutId(): Int {
    return R.layout.dialog_upgrade
  }

  override fun initData() {
    super.initData()
    scope.launch {
      var context = ""
      val fileName = "version_log/version_log_${getVersionSuffix()}.md"
      withContext(Dispatchers.IO) {
        val ins = requireContext().assets.open(fileName)
        context = String(ins.readBytes())
        ins.close()
      }
      RichText.fromMarkdown(context)
          .urlClick { url ->
            if (handlerUrlClick(url)) {
              dismiss()
            }
            return@urlClick true
          }
          .into(binding.tvContent)
    }
    binding.btEnter.setOnClickListener {
      dismiss()
    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.setLayout(360.toPx(), 600.toPx())
  }

  override fun dismiss() {
    super.dismiss()
    requireContext().getSharedPreferences(Constance.PRE_FILE_NAME, Context.MODE_PRIVATE)
        .edit {
          putInt(Constance.VERSION_CODE, AndroidUtils.getVersionCode(requireContext()))
        }
  }

  /**
   * 根据语言获取版本日志后缀名
   */
  private fun getVersionSuffix(): String {
    var defLocal = LanguageUtil.getDefLanguage(requireContext())
    if (defLocal == null) {
      defLocal = LanguageUtil.getSysCurrentLan()
    }
    return if (TextUtils.isEmpty(defLocal.country)) {
      defLocal.language
    } else {
      "${defLocal.language}_${defLocal.country}"
    }
  }

  /**
   * 除了url点击
   * @return true 已处理
   */
  private fun handlerUrlClick(url: String): Boolean {
    val uri = Uri.parse(url)
    if (uri.scheme == "route") {
      val activity = uri.getQueryParameter("activity")
      if (!activity.isNullOrEmpty()) {
        when (activity) {
          "FingerprintActivity" -> {
            if (FingerprintUtil.hasBiometricPrompt(requireContext())) {
              startActivity(
                  Intent(requireContext(), FingerprintActivity::class.java),
                  ActivityOptions.makeSceneTransitionAnimation(requireActivity())
                      .toBundle()
              )
              return true
            }
          }
          "WebDavLoginDialog" -> {
            WebDavLoginDialog().show()
            return true
          }
        }
      }
    } else {
      Log.d(TAG, "url = $url")
      startActivity(Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
      })
    }
    return false
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }

}