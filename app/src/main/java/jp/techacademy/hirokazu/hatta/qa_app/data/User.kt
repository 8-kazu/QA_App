package jp.techacademy.hirokazu.hatta.qa_app.data

data class User(
    val uid: String = "",        // Firebase AuthのUID
    val displayName: String = "" // ユーザーの表示名
)
