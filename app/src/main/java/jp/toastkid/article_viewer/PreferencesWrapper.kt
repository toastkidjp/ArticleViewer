/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer

import android.content.Context
import android.content.SharedPreferences

/**
 * @author toastkidjp
 */
class PreferencesWrapper(context: Context) {

    /**
     * TODO Divide and move package.
     */
    private enum class Key {
        FILE_PATH, LAST_UPDATED
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(javaClass.canonicalName, Context.MODE_PRIVATE)

    fun setTarget(targetPath: String?) {
        if (targetPath == null) {
            return
        }
        preferences.edit()
            .putString(Key.FILE_PATH.name, targetPath)
            .apply()
    }

    fun getTarget(): String? {
        return preferences.getString(Key.FILE_PATH.name, null)
    }

    fun setLastUpdated(ms: Long) {
        preferences.edit().putLong(Key.LAST_UPDATED.name, ms).apply()
    }

    fun getLastUpdated(): Long {
        return preferences.getLong(Key.LAST_UPDATED.name, 0L)
    }
}