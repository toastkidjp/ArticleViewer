package jp.toastkid.article_viewer.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author toastkidjp
 */
class TitleFilterGeneratorTest {

    @Test
    fun test() {
        assertEquals("日記2019-01-01%", TitleFilterGenerator(2019, 1, 1))
        assertEquals("日記2019-10-10%",TitleFilterGenerator(2019, 10, 10))
        assertEquals("日記2019-09-09%",TitleFilterGenerator(2019, 9, 9))
        assertEquals("日記2019-11-11%",TitleFilterGenerator(2019, 11, 11))
    }
}