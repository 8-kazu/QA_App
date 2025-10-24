package jp.techacademy.hirokazu.hatta.qa_app.data

data class Question (
    val questionUid: String = "", // Firebaseのキー
    val title: String = "",
    val body: String = "",
    val name: String = "",       // 投稿者名
    val uid: String = "",        // 投稿者のUID
    val genre: Int = 0,          // ジャンルID
    val imageString: String? = null, // 画像データ (Base64エンコード文字列)
    val answers: List<Answer> = emptyList(),
    val timestamp: Long = 0L,    // 投稿日時（Firestore Timestampのミリ秒）
    val answerCount: Int = 0 ,    // 回答数（リアルタイム更新用）
    val favoritedBy: Map<String, Boolean> = emptyMap() // お気に入り機能用 (将来の拡張)
    //Map は「キーと値」のペアの集まり。
    //
    //String がキー（たとえばユーザーID）、Boolean が値（trueならお気に入り済み）です。
)
