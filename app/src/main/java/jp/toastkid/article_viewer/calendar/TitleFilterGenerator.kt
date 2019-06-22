package jp.toastkid.article_viewer.calendar

/**
 * @author toastkidjp
 */
object TitleFilterGenerator {

    operator fun invoke(year: Int, month: Int, date: Int): String {
        val monthStr = if (month < 10) "0$month" else month.toString()
        val dateStr = if (date < 10) "0$date" else date.toString()
        return "日記$year-$monthStr-$dateStr%"
    }
}