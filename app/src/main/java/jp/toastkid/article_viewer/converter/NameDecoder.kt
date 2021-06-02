/*
 * Copyright (c) 2019 toastkidjp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package jp.toastkid.article_viewer.converter

import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * @author toastkidjp
 */
object NameDecoder {

    private val charset = Charset.forName("EUC-JP")

    operator fun invoke(byteStr: String): String =
        charset.decode(toByteBuffer(toBiCharacters(byteStr))).toString()

    private fun toBiCharacters(byteStr: String): ArrayList<String> {
        byteStr.split("??")
        val strings = ArrayList<String>()
        val temp = StringBuilder(5)
        (byteStr.indices).forEach {
            temp.append(byteStr.toCharArray()[it])
            if (temp.length == 2) {
                strings.add(temp.toString())
                temp.clear()
            }
        }
        return strings
    }

    private fun toByteBuffer(strings: List<String>) =
        ByteBuffer.wrap(
            strings.indices
                .map { strings[it].toLong(16).toByte() }
                .toByteArray()
        )
}