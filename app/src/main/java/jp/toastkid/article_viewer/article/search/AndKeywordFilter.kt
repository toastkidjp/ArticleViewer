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
 * @author toastkidjp
 */
class AndKeywordFilter(keyword: String) {

    private val keywords = keyword.replace("　", " ")
        .split(" ")
        .filterNot { it.isBlank() }
        .toSet()

    operator fun invoke(article: Article) =
        keywords.all { article.title.contains(it) }
                || keywords.all { article.contentText.contains(it) }
}