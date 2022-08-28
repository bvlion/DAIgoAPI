package net.ambitious.daigoapi

import com.atilika.kuromoji.ipadic.Tokenizer

fun createDaiGo(target: String): String {
    words[target]?.let {
        return it
    }
    val tokens = Tokenizer().tokenize(target)
    return tokens.joinToString("") {
        println(it.allFeatures)

        // 助詞、助動詞、記号を除く
        if (listOf("助詞", "助動詞", "記号").contains(it.partOfSpeechLevel1)) {
            return@joinToString ""
        }

        // 〜そう（楽しそう）のような助動詞語幹を除く
        if (listOf("助動詞語幹").contains(it.partOfSpeechLevel3)) {
            return@joinToString ""
        }

        val alphabet = kana2alphabet(it.reading)
        if (alphabet == "*") {
            kana2alphabet(it.surface.subSequence(0, 1).toString())
        } else {
            alphabet
        }
    }
}

private fun kana2alphabet(target: String): String {
    val checkWord = if (target.length == 1) {
        "$target "
    } else {
        target
    }

    return when (checkWord.substring(0, 2)) {
        "ジャ", "ジュ", "ジョ" -> "J"
        else -> when (val word = checkWord.substring(0, 1)) {
            "ア" -> "A"
            "イ" -> "I"
            "ウ" -> "U"
            "エ" -> "E"
            "オ" -> "O"
            "ジ" -> "J"
            "チ" -> "C"
            "カ", "キ", "ク", "ケ", "コ" -> "K"
            "ガ", "ギ", "グ", "ゲ", "ゴ" -> "G"
            "サ", "シ", "ス", "セ", "ソ" -> "S"
            "ザ", "ズ", "ゼ", "ゾ" -> "Z"
            "タ", "ツ", "テ", "ト" -> "T"
            "ダ", "ヂ", "ヅ", "デ", "ド" -> "D"
            "ナ", "ニ", "ヌ", "ネ", "ノ" -> "N"
            "ハ", "ヒ", "フ", "ヘ", "ホ" -> "H"
            "バ", "ビ", "ブ", "ベ", "ボ" -> "B"
            "パ", "ピ", "プ", "ペ", "ポ" -> "P"
            "マ", "ミ", "ム", "メ", "モ" -> "M"
            "ヤ", "ユ", "ヨ" -> "Y"
            "ラ", "リ", "ル", "レ", "ロ" -> "R"
            "ワ" -> "W"
            else -> word
        }
    }
}
