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
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import jp.toastkid.article_viewer.R
import jp.toastkid.article_viewer.common.ProgressCallback
import jp.toastkid.article_viewer.common.SearchFunction
import kotlinx.android.synthetic.main.fragment_content.*

/**
 * @author toastkidjp
 */
class ContentViewerFragment : Fragment(), SearchFunction {

    private var progressCallback: ProgressCallback? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is ProgressCallback) {
            progressCallback = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_content, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        content.text = arguments?.getString("content")
        arguments?.getString("title")?.also {
            progressCallback?.setProgressMessage(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, menuInflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menu?.clear()
        menuInflater?.inflate(R.menu.menu_content_viewer, menu)

        menu?.findItem(R.id.action_to_top_content)?.setOnMenuItemClickListener {
            content_scroll.smoothScrollTo(0, 0)
            true
        }

        menu?.findItem(R.id.action_to_bottom_content)?.setOnMenuItemClickListener {
            content_scroll.smoothScrollTo(0, content.height)
            true
        }
    }

    override fun search(keyword: String?) {
        TextViewHighlighter(content, keyword.toString())
    }

    override fun filter(keyword: String?) = Unit

    companion object {
        fun make(title: String, content: String): Fragment
                = ContentViewerFragment().also {
                    it.arguments = bundleOf(
                        "content" to content,
                        "title" to title
                    )
                }
    }
}