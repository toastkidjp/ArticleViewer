/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.article.search

import jp.toastkid.article_viewer.article.Article

/**
 * Keyword filter for AND search.
 *
 * @param keyword Keyword string
 * @author toastkidjp
 */
class AndKeywordFilter(keyword: String) {

    /**
     * Split specified keyword with white space.
     */
    private val keywords = keyword.replace("　", " ")
        .split(" ")
        .filterNot { it.isBlank() }
        .toSet()

    /**
     * Filter article with keywords.
     *
     * @param article [Article]
     * @return If article's title or content contain all keywords, then return true.
     */
    operator fun invoke(article: Article) =
        keywords.all { article.title.contains(it) }
                || keywords.all { article.content.contains(it) }
}