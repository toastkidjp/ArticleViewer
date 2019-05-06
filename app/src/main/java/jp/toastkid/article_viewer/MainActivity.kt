package jp.toastkid.article_viewer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import jp.toastkid.article_viewer.article.list.ArticleListFragment
import jp.toastkid.article_viewer.zip.FileExtractorFromUri
import jp.toastkid.article_viewer.zip.ZipLoaderService
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.File

class MainActivity : AppCompatActivity(), ProgressCallback {

    private lateinit var articleListFragment: ArticleListFragment

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        articleListFragment = ArticleListFragment()

        input.setOnEditorActionListener { textView, i, keyEvent ->
            articleListFragment.search(textView.text.toString())
            true
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_area, articleListFragment)
        transaction.commit()
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

    private fun selectTargetFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"
        startActivityForResult(intent, 1)
    }

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
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_set_target -> {
                selectTargetFile()
                return true
            }
            else -> super.onOptionsItemSelected(item)
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
