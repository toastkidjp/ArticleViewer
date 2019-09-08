/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.calendar

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.room.Room
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import jp.toastkid.article_viewer.AppDatabase
import jp.toastkid.article_viewer.BuildConfig
import jp.toastkid.article_viewer.R
import jp.toastkid.article_viewer.article.ArticleRepository
import jp.toastkid.article_viewer.article.detail.ContentViewerFragment
import jp.toastkid.article_viewer.common.FragmentControl
import kotlinx.android.synthetic.main.fragment_calendar.*
import timber.log.Timber

/**
 * @author toastkidjp
 */
class CalendarFragment : Fragment() {

    private lateinit var articleRepository: ArticleRepository

    private lateinit var fragmentControl: FragmentControl

    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityContext = context ?: return

        if (activityContext is FragmentControl) {
            fragmentControl = activityContext
        }

        initializeRepository(activityContext)

        setSelectedAction()
    }

    private fun initializeRepository(activityContext: Context) {
        val dataBase = Room.databaseBuilder(
            activityContext.applicationContext,
            AppDatabase::class.java,
            BuildConfig.APPLICATION_ID
        ).build()

        articleRepository = dataBase.diaryRepository()
    }

    private fun setSelectedAction() {
        calendar.setOnDateChangeListener { _, year, month, date ->
            Maybe.fromCallable { articleRepository.findFirst(TitleFilterGenerator(year, month + 1, date)) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        val article = it ?: return@subscribe
                        fragmentControl.replaceFragment(ContentViewerFragment.make(article.title, article.content))
                    },
                    Timber::e
                )
                .addTo(disposables)
        }
    }

    override fun onDetach() {
        super.onDetach()
        disposables.clear()
    }

}