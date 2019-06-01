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
                Article().also {
                    it.title = "android"
                    it.content = "Orange is good"
                }
            )
        )

        assertFalse(
            keywordFilter(
                Article().also {
                    it.title = "iOS all"
                    it.content = "Orange is good"
                }
            )
        )
    }

    @Test
    fun testMultipleKeyword() {
        val keywordFilter = AndKeywordFilter("a b c")

        assertFalse(
            keywordFilter(
                Article().also {
                    it.title = "android"
                    it.content = "Orange is good"
                }
            )
        )

        assertTrue(
            keywordFilter(
                Article().also {
                    it.title = "android"
                    it.content = "I have a book, it is so cool."
                }
            )
        )

    }
}