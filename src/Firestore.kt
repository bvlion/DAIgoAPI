package net.ambitious.daigoapi

import com.fasterxml.jackson.annotation.JsonAlias
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
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

fun save(db: Firestore, saveRequest: SaveRequest): Map<String, String> {
  getDocument(db)
    .set(
      mapOf(saveRequest.word to saveRequest.daiGo),
      SetOptions.merge()
    )

  originalWords.clear()
  setOriginalWords(db)

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
  @JsonAlias("word") val word: String,
  @JsonAlias("dai_go") val daiGo: String
)

val originalWords: HashMap<String, String> = hashMapOf()