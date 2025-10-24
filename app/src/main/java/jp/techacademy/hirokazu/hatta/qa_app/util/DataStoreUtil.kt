package jp.techacademy.hirokazu.hatta.qa_app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import jp.techacademy.hirokazu.hatta.qa_app.data.User
import jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail.QuestionDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import jp.techacademy.hirokazu.hatta.qa_app.Const

object DataStoreUtil
{
// DataStore のインスタンス
 val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
//スマホに小さなノートを作って、アプリの設定（名前とか）を書き込めるように準備している。
//Preferences 型 = 小さな設定（名前やスイッチのON/OFF）用のメモ帳
//preferencesDataStore(name = "settings") は「settings という名前のノートを作るよ」という意味
//by に渡すことで、dataStore にアクセスしたときに自動でそのノートが作られる

    //_uiStateは中身を変える用、uiStateは見る専用。
    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()


// キー定義
private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
//ノートの中の「名前を書く欄」の名前を決めている。

suspend fun getDisplayNameFromDataStore(context: Context): String {//ノートから名前を読むための関数を作った。
    //注意：suspend がついているのは、スマホの処理を少し待つ必要があるから。「呼び出すときちょっと時間がかかるかも」と考えて。

    return context.dataStore.data//先ほど作ったノート (dataStore) から全部のデータを取り出すよ、という準備
        .map { preferences ->
            preferences[DISPLAY_NAME_KEY] ?: ""//ノートの中から「display_name の欄」を見つけて取り出す。
            //?: "" の部分は、「もしまだ書いてなかったら空白にする」という意味。

        }.first()
}

suspend fun saveDisplayNameToDataStore(context: Context, displayName: String) {//ノートに名前を書き込むための関数。
    //displayName が書きたい名前。

    context.dataStore.edit { preferences ->
        preferences[DISPLAY_NAME_KEY] = displayName//ノートの display_name の欄に、実際に名前を書き込む。
    }
}

//お気に入り登録機能
    suspend fun toggleFavorite(//toggleFavorite という関数を宣言。suspend は「非同期処理（コルーチン）内で呼ぶ必要がある」ことを示す。
        question: Question,
        user: User
    ): Boolean {
        val questionsCollection = FirebaseFirestore.getInstance().collection(Const.ContentsPATH)
            .document(question.genre.toString()) // ← 質問のジャンルごとのドキュメント
            .collection("questions")//Firestore というクラウドのデータベースから「questions」という質問の集まり（テーブルみたいなもの）を取り出す

        // 現在のお気に入り状態を取得
        val currentStatus = question.favoritedBy[user.uid] ?: false//question.favoritedByのuser.uidにタイする値を取り出して入れる
        val newStatus = !currentStatus//今お気に入りになっていれば OFF に、OFF なら ON に切り替え

        // favoritedBy のコピーを作って更新
        val updatedFavoritedBy = question.favoritedBy.toMutableMap()  //toMutableMap() は「書き換え可能な地図にする」という意味

        updatedFavoritedBy[user.uid] = newStatus//新しいお気に入り状態を上書きする

        // Firestore に反映
        try {
            questionsCollection.document(question.questionUid)//Firestore の 特定の質問 を指定
                .update("favoritedBy", updatedFavoritedBy) // updateでfavoritedByを更新（updateは既にあるデータを変更したい時に）
                .await()//Firestore への書き込みが 終わるまで待つ
        } catch (e: Exception) {
            // もし update でエラー（フィールドが存在しない等）なら set で上書き
            questionsCollection.document(question.questionUid)
                //(setは新しく作るか、存在しない場合も安全に追加したい時に（ merge を使うと既存データを消さずに更新できる）)
                .set(mapOf("favoritedBy" to updatedFavoritedBy), com.google.firebase.firestore.SetOptions.merge())//既存の他のデータは消さずに、このフィールドだけ追加・更新 する設定
                .await()
        }
        return newStatus//切り替え後の状態（true/false）を呼び出し元に返す。呼び出し元はそれを使って表示などを更新
    }

}