package jp.techacademy.hirokazu.hatta.qa_app

object Const {
    // Firebase Cloud Firestore paths
    const val UsersPATH = "users"           // Firebaseにユーザーの表示名を保存するパス
    const val ContentsPATH = "contents"     // Firebaseに質問を保存するバス
    const val AnswersPATH = "answers"       // Firebaseに回答を保存するパス
    const val NameKEY = "name"              // Firebaseにユーザーの表示名を保存するキー

    // ジャンルID定数
    const val GENRE_HOBBY = 1
    const val GENRE_LIFE = 2
    const val GENRE_HEALTH = 3
    const val GENRE_COMPUTER = 4
}