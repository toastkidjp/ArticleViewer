/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.article.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import jp.toastkid.article_viewer.AppDatabase
import jp.toastkid.article_viewer.BuildConfig
import jp.toastkid.article_viewer.PreferencesWrapper
import jp.toastkid.article_viewer.R
import jp.toastkid.article_viewer.article.ArticleRepository
import jp.toastkid.article_viewer.article.detail.ContentViewerFragment
import jp.toastkid.article_viewer.article.search.AndKeywordFilter
import jp.toastkid.article_viewer.common.FragmentControl
import jp.toastkid.article_viewer.common.ProgressCallback
import jp.toastkid.article_viewer.common.SearchFunction
import jp.toastkid.article_viewer.zip.ZipLoaderService
import kotlinx.android.synthetic.main.fragment_article_list.*
import timber.log.Timber

/**
 * @author toastkidjp
 */
class ArticleListFragment : Fragment(), SearchFunction {

    private lateinit var adapter: Adapter

    private lateinit var preferencesWrapper: PreferencesWrapper

    private lateinit var articleRepository: ArticleRepository

    private val progressBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            progressCallback.hideProgress()
            all()
        }
    }

    private lateinit var progressCallback: ProgressCallback

    private var fragmentControl: FragmentControl? = null

    private val disposables = CompositeDisposable()

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context == null) {
            return
        }

        preferencesWrapper = PreferencesWrapper(context)

        if (context is ProgressCallback) {
            progressCallback = context
        }

        if (context is FragmentControl) {
            fragmentControl = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.let {
            it.registerReceiver(
                progressBroadcastReceiver,
                ZipLoaderService.makeProgressBroadcastIntentFilter()
            )

            initializeRepository(it)
        }

        setHasOptionsMenu(true)
    }

    private fun initializeRepository(activityContext: Context) {
        val dataBase = Room.databaseBuilder(
            activityContext.applicationContext,
            AppDatabase::class.java,
            BuildConfig.APPLICATION_ID
        ).build()

        articleRepository = dataBase.articleRepository()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_article_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityContext = context ?: return

        adapter = Adapter(LayoutInflater.from(activityContext)) { title ->
            Maybe.fromCallable { articleRepository.findContentByTitle(title) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { content ->
                        if (content.isNullOrBlank()) {
                            return@subscribe
                        }
                        fragmentControl?.addFragment(ContentViewerFragment.make(title, content))
                    },
                    Timber::e
                )
                .addTo(disposables)
        }
        results.adapter = adapter
        results.layoutManager = LinearLayoutManager(activityContext, RecyclerView.VERTICAL, false)
    }

    fun all() {
        query(
            Maybe.fromCallable { articleRepository.getAll() }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMapObservable { it.toObservable() }
        )
    }

    override fun search(keyword: String?) {
        if (keyword.isNullOrBlank()) {
            return
        }

        val keywordFilter = AndKeywordFilter(keyword)

        query(
            Maybe.fromCallable { articleRepository.getAllWithContent() }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMapObservable { it.toObservable() }
                .filter { keywordFilter(it) }
                .map { it.toSearchResult() }
        )
    }

    override fun filter(keyword: String?) {
        if (!preferencesWrapper.useTitleFilter()) {
            return
        }

        if (keyword.isNullOrBlank()) {
            all()
            return
        }

        query(
            Maybe.fromCallable { articleRepository.filter("%$keyword%") }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMapObservable { it.toObservable() }
        )
    }

    private fun query(results: Observable<SearchResult>) {
        adapter.clear()
        setSearchStart()

        val start = System.currentTimeMillis()
        results
            .subscribe(
                adapter::add,
                {
                    Timber.e(it)
                    progressCallback.hideProgress()
                },
                { setSearchEnded(System.currentTimeMillis() - start) }
            )
            .addTo(disposables)
    }

    private fun setSearchStart() {
        progressCallback.showProgress()
        progressCallback.setProgressMessage(getString(R.string.message_search_in_progress))
    }

    @UiThread
    private fun setSearchEnded(duration: Long) {
        Completable.fromAction {
            progressCallback.hideProgress()
            adapter.notifyDataSetChanged()
            progressCallback.setProgressMessage("${adapter.itemCount} Articles / $duration[ms]")
        }
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe()
            .addTo(disposables)
    }

    override fun onCreateOptionsMenu(menu: Menu?, menuInflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, menuInflater)
        menuInflater?.inflate(R.menu.menu_article_list, menu)
        menu?.findItem(R.id.action_switch_title_filter)?.isChecked = preferencesWrapper.useTitleFilter()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_all_article -> {
                all()
                true
            }
            R.id.action_to_top -> {
                RecyclerViewScroller.toTop(results)
                true
            }
            R.id.action_to_bottom -> {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        context?.unregisterReceiver(progressBroadcastReceiver)
    }
}