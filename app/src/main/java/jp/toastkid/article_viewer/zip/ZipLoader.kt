package jp.toastkid.article_viewer.zip

import android.os.Build
import jp.toastkid.article_viewer.article.Article
import jp.toastkid.article_viewer.article.ArticleRepository
import jp.toastkid.article_viewer.converter.NameDecoder
import jp.toastkid.article_viewer.tokenizer.NgramTokenizer
import okio.Okio
import timber.log.Timber
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * @author toastkidjp
 */
object ZipLoader {

    private val CHARSET = Charset.forName("UTF-8")

    private val tokenizer = NgramTokenizer()

    private val id = AtomicInteger()

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
                    extract(zipInputStream, nextEntry, articleRepository)
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

    private fun extract(
        zipInputStream: ZipInputStream,
        nextEntry: ZipEntry,
        articleRepository: ArticleRepository
    ) {
        // use() occur java.io.IOException: Stream closed
        Okio.buffer(Okio.source(zipInputStream)).also {
            val content = it.readUtf8()
            val article = Article(id.incrementAndGet()).also { a ->
                a.title = NameDecoder(extractFileName(nextEntry.name))
                a.contentText = content
                a.bigram = tokenizer(content, 2) ?: ""
                a.length = a.contentText.length
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                article.lastModified = nextEntry.lastModifiedTime.to(TimeUnit.MILLISECONDS)
            }
            articleRepository.insert(article)
        }
    }

    private fun extractFileName(name: String) = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."))

}