/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.diary.content.viewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.toastkid.diary.R
import kotlinx.android.synthetic.main.activity_content.*

/**
 * @author toastkidjp
 */
class ContentViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)
        content.text = intent.getStringExtra("content")
    }

    companion object {
        fun makeIntent(context: Context, content: String) =
            Intent(context, ContentViewerActivity::class.java)
                .also { it.putExtra("content", content) }
    }
}