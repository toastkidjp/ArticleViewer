package jp.toastkid.article_viewer.article.search

import jp.toastkid.article_viewer.article.Article
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * @author toastkidjp
 */
class AndKeywordFilterTest {

    @Test
    fun testSingleKeyword() {
        val keywordFilter = AndKeywordFilter("and")

        assertTrue(
            keywordFilter(
                Article(4).also {
                    it.title = "android"
                    it.contentText = "Orange is good"
                }
            )
        )

        assertFalse(
            keywordFilter(
                Article(3).also {
                    it.title = "iOS all"
                    it.contentText = "Orange is good"
                }
            )
        )
    }

    @Test
    fun testMultipleKeyword() {
        val keywordFilter = AndKeywordFilter("a b c")

        assertFalse(
            keywordFilter(
                Article(1).also {
                    it.title = "android"
                    it.contentText = "Orange is good"
                }
            )
        )

        assertTrue(
            keywordFilter(
                Article(2).also {
                    it.title = "android"
                    it.contentText = "I have a book, it is so cool."
                }
            )
        )

    }
}