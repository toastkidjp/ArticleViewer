/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.zip

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.JobIntentService
import androidx.core.net.toUri
import androidx.room.Room
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.toastkid.article_viewer.AppDatabase
import jp.toastkid.article_viewer.BuildConfig
import jp.toastkid.article_viewer.PreferencesWrapper
import okio.Okio
import timber.log.Timber
import java.io.File

/**
 * @author toastkidjp
 */
class ZipLoaderService : JobIntentService() {

    @SuppressLint("CheckResult")
    override fun onHandleWork(intent: Intent) {
        val dataBase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            BuildConfig.APPLICATION_ID
        ).build()

        val articleRepository = dataBase.diaryRepository()

        val file = File(FileExtractorFromUri(this, intent.getStringExtra("target").toUri()))

        Completable.fromAction {
            ZipLoader.invoke(
                Okio.buffer(Okio.source(file)).inputStream(),
                articleRepository
            )
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    PreferencesWrapper(this).setLastUpdated(file.lastModified())
                    /*progress.visibility = View.GONE
                    progress_circular.visibility = View.GONE
                    all()*/
                    val progressIntent = Intent(ACTION_PROGRESS_BROADCAST)
                    progressIntent.putExtra("progress", 100)
                    sendBroadcast(progressIntent)
                },
                {
                    Timber.e(it)
                    /*progress.visibility = View.GONE
                    progress_circular.visibility = View.GONE*/
                }
            )
    }

    companion object {

        private const val ACTION_PROGRESS_BROADCAST = "jp.toastkid.articles.importing.progress"

        fun makeProgressBroadcastIntentFilter() = IntentFilter(ACTION_PROGRESS_BROADCAST)

        fun start(context: Context, target: String) {
            val intent = Intent(context, ZipLoaderService::class.java)
            intent.putExtra("target", target)
            enqueueWork(context, ZipLoaderService::class.java, 20, intent)
        }
    }
}