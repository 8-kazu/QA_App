package jp.techacademy.hirokazu.hatta.qa_app.data

data class Answer (
    val answerUid: String = "", // Firebaseのキー
    val body: String = "",
    val name: String = "",    // 回答者名
    val uid: String = ""      // 回答者のUID
)