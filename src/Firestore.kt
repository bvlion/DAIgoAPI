package net.ambitious.daigoapi

import com.fasterxml.jackson.annotation.JsonAlias
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.SetOptions
import com.google.cloud.firestore.WriteResult
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.slf4j.Logger
import java.io.ByteArrayInputStream

fun firestore(credentials: ByteArray?): Firestore =
    if (credentials == null) {
        FirestoreOptions
            .newBuilder()
            .setProjectId("test")
            .setEmulatorHost("localhost:8081")
            .build()
            .service
    } else {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(credentials)))
            .build()
        FirebaseApp.initializeApp(options)
        FirestoreClient.getFirestore()
    }

fun save(db: Firestore, saveRequest: SaveRequest, log: Logger): Map<String, String> {
    ApiFutures.addCallback(
        getDocument(db)
            .set(
                mapOf(saveRequest.word to saveRequest.daiGo),
                SetOptions.merge()
            ),
        object : ApiFutureCallback<WriteResult> {

            override fun onFailure(t: Throwable?) {
                log.warn("firestore save error", t)
            }

            override fun onSuccess(result: WriteResult?) {
                originalWords.clear()
                setOriginalWords(db)
            }
        }
    ) {
        it.run()
    }

    return mapOf("save" to "success")
}

fun setOriginalWords(db: Firestore) {
    getDocument(db).get().get()
        .data?.map { mapOf(it.key to it.value.toString()) }?.forEach {
            originalWords.putAll(it)
        }
}

fun setWords(db: Firestore) {
    val samples = getDocument(db, "sample").get().get().data
    if (samples != null) {
        sampleWords.clear()
        samples.map { mapOf(it.key to it.value.toString()) }.forEach {
            sampleWords.addAll(it.values)
        }
    }
    val notExists = getDocument(db, "notExists").get().get().data
    if (notExists != null) {
        notExistWords.clear()
        notExistWords.addAll(
            notExists
                .map { it.key.toInt() to it.value }
                .sortedBy { it.first }
                .map { it.second as Map<*, *> }
                .flatMap { map ->
                    map.map { it.key.toString() to it.value.toString() }
                }
        )
    }
}

private fun getDocument(db: Firestore, document: String = "words"): DocumentReference =
    db.collection("list").document(document)

data class SaveRequest(
    @JsonAlias("word") val word: String?,
    @JsonAlias("dai_go") val daiGo: String?
)

val words: Map<String, String>
    get() = originalWords
private val originalWords = hashMapOf<String, String>()

val notExists: List<Pair<String, String>>
    get() = notExistWords
private val notExistWords = arrayListOf(
    "からあげ" to "K",
    "から揚げ" to "K",
    "唐揚げ" to "K",
    "唐揚" to "K"
)

val samples: List<String>
    get() = sampleWords
private val sampleWords = arrayListOf(
    "努力大事",
    "DAIGO大誤算",
    "グイグイヨシコイ",
    "上昇志向",
    "負ける気がしない"
)
