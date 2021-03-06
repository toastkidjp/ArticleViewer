/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.common

/**
 * Search function callbacks.
 *
 * @author toastkidjp
 */
interface SearchFunction {

    /**
     * Search action with keyword string.
     *
     * @param keyword nullable string
     */
    fun search(keyword: String?)

    /**
     * Filter action with keyword string.
     *
     * @param keyword nullable string
     */
    fun filter(keyword: String?)

}