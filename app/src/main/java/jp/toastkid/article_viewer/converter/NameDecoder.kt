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

    operator fun invoke(byteStr: String): String {
        val strings = ArrayList<String>()
        val temp = StringBuilder(5)
        for (i in 0 until byteStr.length) {
            temp.append(byteStr.toCharArray()[i])
            if (temp.length == 2) {
                strings.add(temp.toString())
                temp.delete(0, temp.length)
            }
        }

        val b = ByteArray(byteStr.length / 2)
        for (i in strings.indices) {
            b[i] = strings[i].toLong(16).toByte()
        }
        val byteBuffer = ByteBuffer.wrap(b)
        return charset.decode(byteBuffer).toString()
    }
}