package jp.toastkid.article_viewer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import jp.toastkid.article_viewer.article.list.ArticleListFragment
import jp.toastkid.article_viewer.bookmark.BookmarkFragment
import jp.toastkid.article_viewer.calendar.CalendarFragment
import jp.toastkid.article_viewer.common.FragmentControl
import jp.toastkid.article_viewer.common.ProgressCallback
import jp.toastkid.article_viewer.common.SearchFunction
import jp.toastkid.article_viewer.zip.FileExtractorFromUri
import jp.toastkid.article_viewer.zip.ZipLoaderService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_content.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Main activity of this app.
 *
 * @author toastkidjp
 */
class MainActivity : AppCompatActivity(), ProgressCallback, FragmentControl {

    /**
     * Article list fragment.
     */
    private lateinit var articleListFragment: ArticleListFragment

    private lateinit var calendarFragment: CalendarFragment

    private lateinit var bookmarkFragment: BookmarkFragment

    /**
     * Search function it's invoked from text field.
     */
    private var searchFunction: SearchFunction? = null

    /**
     * Use for handling input action.
     */
    private val inputSubject = PublishSubject.create<String>()

    /**
     * Disposables.
     */
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        articleListFragment = ArticleListFragment()

        input.setOnEditorActionListener { textView, _, _ ->
            val keyword = textView.text.toString()
            if (keyword.isBlank()) {
                return@setOnEditorActionListener true
            }
            getCurrentSearchFunction()?.search(keyword)
            true
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) = Unit

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) = Unit

            override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                inputSubject.onNext(charSequence.toString())
            }

        })

        inputSubject.distinctUntilChanged()
            .debounce(1400L, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { getCurrentSearchFunction()?.filter(it) }
            .addTo(disposables)

        setFragment(articleListFragment)
    }

    private fun getCurrentSearchFunction(): SearchFunction? {
        val supportFragmentManager = supportFragmentManager ?: return null
        val fragment = supportFragmentManager.fragments[supportFragmentManager.backStackEntryCount - 1]
        return if (fragment is SearchFunction) fragment else null
    }

    /**
     * Set fragment to content area.
     *
     * @param fragment [Fragment]
     */
    private fun setFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_area, fragment)
        transaction.addToBackStack(fragment::class.java.canonicalName)
        transaction.commit()

        extractSearchFunction(fragment)
    }

    /**
     * Extract function callback from fragment.
     *
     * @param fragment [Fragment]
     */
    private fun extractSearchFunction(fragment: Fragment) {
        if (fragment is SearchFunction) {
            searchFunction = fragment
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val preferencesWrapper = PreferencesWrapper(this)
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
    }

    /**
     * Select target zip file.
     */
    private fun selectTargetFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"
        startActivityForResult(intent, 1)
    }

    /**
     * Update list if need.
     */
    private fun updateIfNeed() {
        val preferencesWrapper = PreferencesWrapper(this)
        val target = preferencesWrapper.getTarget()
        if (target.isNullOrBlank()) {
            return
        }

        val file = File(FileExtractorFromUri(this, target.toUri()))
        if (preferencesWrapper.getLastUpdated() == file.lastModified()) {
            articleListFragment.all()
            return
        }

        showProgress()

        ZipLoaderService.start(this, target)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_set_target -> {
                selectTargetFile()
                return true
            }
            R.id.action_calendar -> {
                if (!::calendarFragment.isInitialized) {
                    calendarFragment = CalendarFragment()
                }
                replaceFragment(calendarFragment)
                return true
            }
            R.id.action_bookmark -> {
                if (PreferencesWrapper(this).bookmark().isEmpty()) {
                    Snackbar.make(content, "Bookmark is empty.", Snackbar.LENGTH_SHORT).show()
                    return true
                }

                if (!::bookmarkFragment.isInitialized) {
                    bookmarkFragment = BookmarkFragment()
                }
                replaceFragment(bookmarkFragment)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
            return
        }

        supportFragmentManager.fragments.let {
            val fragment = it[it.size - 1]
            if (fragment is SearchFunction) {
                searchFunction = fragment
            }
        }
    }

    override fun showProgress() {
        progress.progress = 0
        progress.visibility = View.VISIBLE
        progress_circular.visibility = View.VISIBLE
    }

    override fun hideProgress() {
        progress.visibility = View.GONE
        progress_circular.visibility = View.GONE
    }

    override fun setProgressMessage(message: String) {
        search_result.also { it.text = message }
    }

    override fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_area, fragment)
        transaction.addToBackStack(fragment::class.java.canonicalName)
        transaction.commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            PreferencesWrapper(this).setTarget(data?.data?.toString())
            updateIfNeed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

}
