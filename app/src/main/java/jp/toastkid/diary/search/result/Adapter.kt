/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.diary.search.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jp.toastkid.diary.R

/**
 * @author toastkidjp
 */
class Adapter(private val layoutInflater: LayoutInflater) : RecyclerView.Adapter<ViewHolder>() {

    private val items: MutableList<DictionaryFile> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.item_result, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dictionaryFile = items[position]
        holder.setTitle(dictionaryFile.title)
        holder.setOnClick(dictionaryFile)
    }

    fun replace(results: List<DictionaryFile>) {
        items.clear()
        items.addAll(results)
        notifyDataSetChanged()
    }
}