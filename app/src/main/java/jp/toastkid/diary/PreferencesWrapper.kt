/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.diary

import android.content.Context
import android.content.SharedPreferences

/**
 * @author toastkidjp
 */
class PreferencesWrapper(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(javaClass.canonicalName, Context.MODE_PRIVATE)

    fun setTarget(targetPath: String?) {
        if (targetPath == null) {
            return
        }
        preferences.edit()
            .putString("path", targetPath)
            .apply()
    }

    fun getTarget(): String? {
        return preferences.getString("path", null)
    }
}