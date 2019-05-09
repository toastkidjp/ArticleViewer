/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.article.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import jp.toastkid.article_viewer.R
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.android.synthetic.main.module_page_searcher.*

/**
 * @author toastkidjp
 */
class ContentViewerActivity : AppCompatActivity() {

    private lateinit var pageSearcher: PageSearcherModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        toolbar.inflateMenu(R.menu.menu_content_viewer)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_close -> {
                    finish()
                    true
                }
                R.id.action_find -> {
                    page_searcher.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
        content.text = intent.getStringExtra("content")
        toolbar.title = intent.getStringExtra("title")

        initializePageSearcher()
    }

    private fun initializePageSearcher() {
        pageSearcher = PageSearcherModule(input, content)
        find_clear.setOnClickListener { pageSearcher.clearInput() }
        find_close.setOnClickListener { page_searcher.visibility = View.GONE }
        find_upward.setOnClickListener { pageSearcher.find() }
        find_downward.setOnClickListener { pageSearcher.find() }
    }

    companion object {
        fun makeIntent(context: Context, title: String, content: String) =
            Intent(context, ContentViewerActivity::class.java)
                .also {
                    it.putExtra("title", title)
                    it.putExtra("content", content)
                }
    }
}