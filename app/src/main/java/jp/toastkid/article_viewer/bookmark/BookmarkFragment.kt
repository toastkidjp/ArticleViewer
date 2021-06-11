/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.bookmark

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import jp.toastkid.article_viewer.AppDatabase
import jp.toastkid.article_viewer.BuildConfig
import jp.toastkid.article_viewer.PreferencesWrapper
import jp.toastkid.article_viewer.R
import jp.toastkid.article_viewer.article.ArticleRepository
import jp.toastkid.article_viewer.article.detail.ContentViewerFragment
import jp.toastkid.article_viewer.article.list.Adapter
import jp.toastkid.article_viewer.article.list.RecyclerViewScroller
import jp.toastkid.article_viewer.article.list.SearchResult
import jp.toastkid.article_viewer.common.FragmentControl
import kotlinx.android.synthetic.main.fragment_bookmark_list.*
import timber.log.Timber

/**
 * @author toastkidjp
 */
class BookmarkFragment : Fragment() {

    /**
     * [RecyclerView]'s adapter.
     */
    private lateinit var adapter: Adapter

    /**
     * Preferences wrapper.
     */
    private lateinit var preferencesWrapper: PreferencesWrapper

    /**
     * Use for reading article data from DB.
     */
    private lateinit var articleRepository: ArticleRepository

    /**
     * Use for switching fragments.
     */
    private var fragmentControl: FragmentControl? = null

    /**
     * Use for clean up subscriptions.
     */
    private val disposables = CompositeDisposable()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context == null) {
            return
        }

        preferencesWrapper = PreferencesWrapper(context)

        if (context is FragmentControl) {
            fragmentControl = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.let { initializeRepository(it) }

        setHasOptionsMenu(true)
    }

    /**
     * Initialize repository.
     *
     * @param activityContext [Context]
     */
    private fun initializeRepository(activityContext: Context) {
        val dataBase = Room.databaseBuilder(
            activityContext.applicationContext,
            AppDatabase::class.java,
            BuildConfig.APPLICATION_ID
        ).build()

        articleRepository = dataBase.articleRepository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_bookmark_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityContext = context ?: return

        adapter = Adapter(
            LayoutInflater.from(activityContext),
            { title ->
                Maybe.fromCallable { articleRepository.findContentByTitle(title) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { content ->
                            if (content.isNullOrBlank()) {
                                return@subscribe
                            }
                            fragmentControl?.replaceFragment(ContentViewerFragment.make(title, content))
                        },
                        Timber::e
                    )
                    .addTo(disposables)
            },
            { }
        )
        results.adapter = adapter
        preferencesWrapper.bookmark().forEach { adapter.add(SearchResult(it, 0, 0)) }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menuInflater?.inflate(R.menu.menu_article_list, menu)
        menu?.findItem(R.id.action_switch_title_filter)?.isChecked =
            preferencesWrapper.useTitleFilter()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_to_top_content -> {
            RecyclerViewScroller.toTop(results)
            true
        }
        R.id.action_to_bottom_content -> {
            RecyclerViewScroller.toBottom(results)
            true
        }
        R.id.action_switch_title_filter -> {
            val newState = !item.isChecked
            preferencesWrapper.switchUseTitleFilter(newState)
            item.isChecked = newState
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDetach() {
        super.onDetach()
        disposables.clear()
    }

}