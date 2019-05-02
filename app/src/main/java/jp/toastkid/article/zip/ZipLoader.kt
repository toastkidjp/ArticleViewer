package jp.toastkid.article.zip

import android.os.Build
import jp.toastkid.article.article.Article
import jp.toastkid.article.article.ArticleRepository
import jp.toastkid.article.converter.NameDecoder
import okio.Okio
import timber.log.Timber
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * @author toastkidjp
 */
object ZipLoader {

    private val CHARSET = Charset.forName("UTF-8")

    operator fun invoke(
        inputStream: InputStream,
        articleRepository: ArticleRepository
        ) {
        ZipInputStream(inputStream, CHARSET)
            .also { zipInputStream ->
                var nextEntry = zipInputStream.nextEntry
                while (nextEntry != null) {
                    if (!nextEntry.name.contains(".")) {
                        nextEntry = zipInputStream.nextEntry
                        continue
                    }
                    Okio.buffer(Okio.source(zipInputStream)).also {
                        val content = it.readUtf8()
                        val article = Article().also { a ->
                            a.title = NameDecoder(extractFileName(nextEntry.name))
                            a.content = content
                            a.length = a.content.codePoints().sum()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            article.lastModified = nextEntry.lastModifiedTime.to(TimeUnit.MILLISECONDS)
                        }
                        articleRepository.insert(article)
                    }
                    nextEntry = try {
                        zipInputStream.nextEntry
                    } catch (e: IllegalArgumentException) {
                        Timber.e("illegal: ${nextEntry.name}")
                        Timber.e(e)
                        return
                    }
                }
                zipInputStream.closeEntry()
            }
    }

    private fun extractFileName(name: String) = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."))

}