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
import java.io.FileInputStream

fun firestore(databaseUrl: String): Firestore =
  if (databaseUrl.isEmpty()) {
    FirestoreOptions
      .newBuilder()
      .setProjectId("test")
      .setEmulatorHost("localhost:8081")
      .build()
      .service
  } else {
    val options = FirebaseOptions.builder()
      .setCredentials(GoogleCredentials.fromStream(FileInputStream("firebase-adminsdk.json")))
      .setDatabaseUrl(databaseUrl)
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
      ), object : ApiFutureCallback<WriteResult> {

      override fun onFailure(t: Throwable?) {
        log.warn("firestore save error", t)
      }

      override fun onSuccess(result: WriteResult?) {
        originalWords.clear()
        setOriginalWords(db)
      }

      }) {
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

private fun getDocument(db: Firestore): DocumentReference =
  db.collection("list").document("words")

data class SaveRequest(
  @JsonAlias("word") val word: String?,
  @JsonAlias("dai_go") val daiGo: String?
)

val originalWords: HashMap<String, String> = hashMapOf()