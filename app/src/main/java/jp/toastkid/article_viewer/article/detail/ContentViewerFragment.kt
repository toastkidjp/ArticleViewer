/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.article.detail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import jp.toastkid.article_viewer.R
import jp.toastkid.article_viewer.common.SearchFunction
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.android.synthetic.main.module_page_searcher.*

/**
 * @author toastkidjp
 */
class ContentViewerActivity : Fragment(), SearchFunction {

    private lateinit var pageSearcher: PageSearcherModule

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.activity_content, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        content.text = arguments?.getString("content")
        initializePageSearcher()
    }

    private fun initializePageSearcher() {
        pageSearcher = PageSearcherModule(input, content)
        find_clear.setOnClickListener { pageSearcher.clearInput() }
        find_close.setOnClickListener { page_searcher.visibility = View.GONE }
        find_upward.setOnClickListener { pageSearcher.find() }
        find_downward.setOnClickListener { pageSearcher.find() }
    }

    override fun search(keyword: String?) {
        TextViewHighlighter(content, keyword.toString())
    }

    companion object {
        fun make(context: Context, title: String, content: String): Fragment
                = ContentViewerActivity().also {
                    it.arguments = bundleOf(
                        "content" to content,
                        "title" to title
                    )
                }
    }
}