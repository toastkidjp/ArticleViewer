package jp.toastkid.article_viewer.calendar

/**
 * Title filtering query generator.
 *
 * @author toastkidjp
 */
class TitleFilterGenerator {

    /**
     * Make filtering query with year, month, and date.
     *
     * @param year ex) 2019
     * @param month You should specify 1-12
     * @param date You should specify 1-31
     *
     * @return Title filter string
     */
    operator fun invoke(year: Int, month: Int, date: Int): String {
        val monthStr = formatDigit(month)
        val dateStr = formatDigit(date)
        return "$year-$monthStr-$dateStr%"
    }

    /**
     * If you passed under 10 digit, this function will return with added 0.
     * @param digit any digit
     * @return string
     */
    private fun formatDigit(digit: Int) = if (digit < 10) "0$digit" else digit.toString()

}