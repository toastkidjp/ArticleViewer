package jp.toastkid.article_viewer

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import jp.toastkid.article_viewer.article.ArticleRepository
import jp.toastkid.article_viewer.article.detail.ContentViewerActivity
import jp.toastkid.article_viewer.article.list.Adapter
import jp.toastkid.article_viewer.article.list.RecyclerViewScroller
import jp.toastkid.article_viewer.article.list.SearchResult
import jp.toastkid.article_viewer.article.search.AndKeywordFilter
import jp.toastkid.article_viewer.zip.FileExtractorFromUri
import jp.toastkid.article_viewer.zip.ZipLoaderService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: Adapter

    private lateinit var preferencesWrapper: PreferencesWrapper

    private lateinit var articleRepository: ArticleRepository

    private val progressBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            hideProgress()
            all()
        }
    }

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        preferencesWrapper = PreferencesWrapper(this)

        adapter = Adapter(LayoutInflater.from(this)) { title ->
            Maybe.fromCallable { articleRepository.findContentByTitle(title) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { content ->
                        if (content.isNullOrBlank()) {
                            return@subscribe
                        }
                        startActivity(ContentViewerActivity.makeIntent(this, title, content))
                    },
                    Timber::e
                )
                .addTo(disposables)
        }
        results.adapter = adapter
        results.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        registerReceiver(progressBroadcastReceiver, ZipLoaderService.makeProgressBroadcastIntentFilter())

        val dataBase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            BuildConfig.APPLICATION_ID
        ).build()
        articleRepository = dataBase.diaryRepository()

        RxPermissions(this)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .subscribe(
                {
                    if (!it) {
                        finish()
                        return@subscribe
                    }
                    if (TextUtils.isEmpty(preferencesWrapper.getTarget())) {
                        selectTargetFile()
                        return@subscribe
                    }
                    updateIfNeed()
                },
                Timber::e
            )
            .addTo(disposables)

        input.setOnEditorActionListener { textView, i, keyEvent ->
            search(textView.text.toString())
            true
        }
    }

    private fun updateIfNeed() {
        val target = preferencesWrapper.getTarget()
        if (target.isNullOrBlank()) {
            return
        }

        val file = File(FileExtractorFromUri(this, target.toUri()))
        if (preferencesWrapper.getLastUpdated() == file.lastModified()) {
            all()
            return
        }

        progress.progress = 0
        progress.visibility = View.VISIBLE
        progress_circular.visibility = View.VISIBLE

        ZipLoaderService.start(this, target)
    }

    private fun selectTargetFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"
        startActivityForResult(intent, 1)
    }

    private fun all() {
        query(
            Maybe.fromCallable { articleRepository.getAll() }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMapObservable { it.toObservable() }
        )
    }

    private fun search(keyword: String?) {
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

    private fun query(results: Observable<SearchResult>) {
        setSearchStart()

        val start = System.currentTimeMillis()
        results
            .subscribe(
                adapter::add,
                {
                    Timber.e(it)
                    hideProgress()
                },
                { setSearchEnded(System.currentTimeMillis() - start) }
            )
            .addTo(disposables)
    }

    private fun setSearchStart() {
        adapter.clear()

        progress.progress = 0
        progress.visibility = View.VISIBLE
        progress_circular.visibility = View.VISIBLE

        search_result.setText(R.string.message_search_in_progress)
    }

    @UiThread
    private fun setSearchEnded(duration: Long) {
        Completable.fromAction {
            hideProgress()
            adapter.notifyDataSetChanged()
            search_result.also {
                it.text = "${adapter.itemCount} Articles / $duration[ms]"
            }
        }
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe()
            .addTo(disposables)
    }

    private fun hideProgress() {
        progress.visibility = View.GONE
        progress_circular.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_all_article -> {
                all()
                true
            }
            R.id.action_settings -> true
            R.id.action_to_top -> {
                RecyclerViewScroller.toTop(results)
                true
            }
            R.id.action_to_bottom -> {
                RecyclerViewScroller.toBottom(results)
                true
            }
            R.id.action_set_target -> {
                selectTargetFile()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            preferencesWrapper.setTarget(data?.data?.toString())
            updateIfNeed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
        unregisterReceiver(progressBroadcastReceiver)
    }
}
