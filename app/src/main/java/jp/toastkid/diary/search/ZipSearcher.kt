package jp.toastkid.diary.search

import jp.toastkid.diary.converter.NameDecoder
import jp.toastkid.diary.search.result.DictionaryFile
import okio.Okio
import timber.log.Timber
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream


/**
 * @author toastkidjp
 */
object ZipSearcher {

    operator fun invoke(inputStream: InputStream, keyword: String): List<DictionaryFile> {
        val results: MutableList<DictionaryFile> = mutableListOf()

        ZipInputStream(inputStream, Charset.forName("UTF-8"))
            .also { zipInputStream ->
                var nextEntry = zipInputStream.nextEntry
                Timber.i("next file: ${nextEntry.name}")
                while (nextEntry != null) {
                    Okio.buffer(Okio.source(zipInputStream)).also {
                        val content = it.readUtf8()
                        if (content.contains(keyword)) {
                            val name = nextEntry.name
                            val title =
                                NameDecoder(name.substring(name.indexOf("/") + 1, name.lastIndexOf(".")))
                            results.add(DictionaryFile(title, content))
                            //Timber.i(nextEntry.name + ": " + if (content.length < 100) { content } else {content.substring(0, 100) })
                        }
                    }
                    nextEntry = try {
                        zipInputStream.nextEntry
                    } catch (e: IllegalArgumentException) {
                        Timber.e("illegal: ${nextEntry.name}")
                        Timber.e(e)
                        return results
                    }
                }
                zipInputStream.closeEntry()
            }
        return results
/*
try (ZipInputStream zipIn = new ZipInputStream(inputStream,Charset.forName("SJIS"))) {

        // zip内のファイルがなくなるまで読み続ける
        while (null != zipIn.getNextEntry()) {

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(zipIn, "Shift_JIS"));
            while(null != (line = reader.readLine())){

                String[] data = line.split(":");

                String fruit = data[0];
                int value = Integer.parseInt(data[1]);

                // すでに果物があれば加える。なければ、そのまま代入する。
                result.computeIfPresent(fruit, (k,v) -> v + value);
                result.putIfAbsent(fruit, value);
            }
            zipIn.closeEntry();
        }
    }
 */
    }

}