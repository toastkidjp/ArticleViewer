/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article.search.result

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import jp.toastkid.article.R
import jp.toastkid.article.article.Article
import jp.toastkid.article.content.viewer.ContentViewerActivity

/**
 * @author toastkidjp
 */
class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    fun setTitle(title: String) {
        view.findViewById<TextView>(R.id.main_text).text = title
    }

    fun setOnClick(dictionaryFile: Article) {
        view.setOnClickListener {
            view.context?.let {
                it.startActivity(ContentViewerActivity.makeIntent(it, dictionaryFile.title, dictionaryFile.content))
            }
        }
    }

}