/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.article

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * @author toastkidjp
 */
@Dao
interface ArticleRepository {

    @Query("SELECT * FROM article ORDER BY lastModified DESC LIMIT 500")
    fun getAll(): List<Article>

    @Query("SELECT * FROM article WHERE title LIKE :keyword OR content LIKE :keyword ORDER BY title DESC")
    fun search(keyword: String): List<Article>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: Article)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg entities: Article)

    @Query("DELETE FROM article")
    fun deleteAll()
}