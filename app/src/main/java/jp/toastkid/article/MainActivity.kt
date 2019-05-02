package jp.toastkid.article

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import jp.toastkid.article.article.Article
import jp.toastkid.article.article.ArticleRepository
import jp.toastkid.article.search.result.Adapter
import jp.toastkid.article.zip.ZipLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okio.Okio
import timber.log.Timber
import java.io.File
import java.util.concurrent.Callable

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: Adapter

    private lateinit var preferencesWrapper: PreferencesWrapper

    private lateinit var articleRepository: ArticleRepository

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        preferencesWrapper = PreferencesWrapper(this)

        adapter = Adapter(LayoutInflater.from(this))
        results.adapter = adapter
        results.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

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

        input.editText?.setOnEditorActionListener { textView, i, keyEvent ->
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
                    progress.visibility = View.GONE
                    progress_circular.visibility = View.GONE
                    preferencesWrapper.setLastUpdated(file.lastModified())
                    all()
                },
                {
                    Timber.e(it)
                    progress.visibility = View.GONE
                    progress_circular.visibility = View.GONE
                }
            )
            .addTo(disposables)
    }

    private fun selectTargetFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"
        startActivityForResult(intent, 1)
    }

    private fun all() {
        query(Callable { articleRepository.getAll() })
    }

    private fun search(keyword: String) {
        query(Callable { articleRepository.search("%$keyword%") })
    }

    private fun query(callable: Callable<List<Article>>) {
        adapter.clear()

        progress.progress = 0
        progress.visibility = View.VISIBLE
        progress_circular.visibility = View.VISIBLE

        Maybe.fromCallable(callable)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    adapter.replace(it)
                    progress.visibility = View.GONE
                    progress_circular.visibility = View.GONE
                },
                {
                    Timber.e(it)
                    progress.visibility = View.GONE
                    progress_circular.visibility = View.GONE
                }
            )
            .addTo(disposables)
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
    }
}
