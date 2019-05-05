/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.article.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jp.toastkid.article_viewer.R

/**
 * @author toastkidjp
 */
class Adapter(
    private val layoutInflater: LayoutInflater,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    private val items: MutableList<Result> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.item_result, parent, false), onClick)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun replace(results: List<Result>) {
        clear()
        items.addAll(results)
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
    }

    fun add(result: Result) {
        items.add(result)
    }
}