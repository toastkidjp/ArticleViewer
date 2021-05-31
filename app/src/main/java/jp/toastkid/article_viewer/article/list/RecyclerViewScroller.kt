package jp.toastkid.article_viewer.article.list

import androidx.recyclerview.widget.RecyclerView

/**
 * @author toastkidjp
 */
object RecyclerViewScroller {

    private const val THRESHOLD: Int = 30

    fun toTop(recyclerView: RecyclerView) {
        if (recyclerView.adapter?.itemCount ?: 0 > THRESHOLD) {
            recyclerView.scrollToPosition(0)
            return
        }
        recyclerView.post { recyclerView.smoothScrollToPosition(0) }
    }

    fun toBottom(recyclerView: RecyclerView) {
        val itemCount = recyclerView.adapter?.itemCount ?: return

        if (itemCount <= 0) {
            return
        }

        if (itemCount > THRESHOLD) {
            recyclerView.scrollToPosition(itemCount - 1)
            return
        }
        recyclerView.post { recyclerView.smoothScrollToPosition(itemCount - 1) }
    }
}