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
import com.google.android.material.snackbar.Snackbar
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
import jp.toastkid.article_viewer.common.FragmentControl
import jp.toastkid.article_viewer.common.ProgressCallback
import jp.toastkid.article_viewer.common.SearchFunction
import jp.toastkid.article_viewer.tokenizer.NgramTokenizer
import jp.toastkid.article_viewer.zip.ZipLoaderService
import kotlinx.android.synthetic.main.fragment_article_list.*
import timber.log.Timber

/**
 * Article list fragment.
 *
 * @author toastkidjp
 */
class ArticleListFragment : Fragment(), SearchFunction {

    /**
     * List item adapter.
     */
    private lateinit var adapter: Adapter

    /**
     * Preferences wrapper.
     */
    private lateinit var preferencesWrapper: PreferencesWrapper

    /**
     * Use for read articles from DB.
     */
    private lateinit var articleRepository: ArticleRepository

    /**
     * Use for receiving broadcast.
     */
    private val progressBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            progressCallback.hideProgress()
            all()
        }
    }

    /**
     * Progress callback.
     */
    private lateinit var progressCallback: ProgressCallback

    /**
     * Use for switching fragment.
     */
    private var fragmentControl: FragmentControl? = null

    /**
     * [CompositeDisposable].
     */
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

        adapter = Adapter(
            LayoutInflater.from(context),
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
            {
                if (preferencesWrapper.containsBookmark(it)) {
                    Snackbar.make(results, "「$it」 is already added.", Snackbar.LENGTH_SHORT).show()
                    return@Adapter
                }
                preferencesWrapper.addBookmark(it)
                Snackbar.make(results, "It has added $it.", Snackbar.LENGTH_SHORT).show()
            }
        )

        retainInstance = true
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

        articleRepository = dataBase.diaryRepository()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_article_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityContext = context ?: return
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

        val tokenizer = NgramTokenizer()

        query(
            Maybe.fromCallable { articleRepository.search("${tokenizer(keyword, 2)}") }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMapObservable { it.toObservable() }
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
            .doOnTerminate { setSearchEnded(System.currentTimeMillis() - start) }
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