package jp.techacademy.hirokazu.hatta.qa_app.ui.answersend

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import jp.techacademy.hirokazu.hatta.qa_app.Const
import jp.techacademy.hirokazu.hatta.qa_app.data.Answer
import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.getDisplayNameFromDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AnswerSendViewModel: ViewModel() {//「回答送信画面」専用のViewModelクラスを作るよ、という宣言。
//ログイン中のユーザー情報を扱うためのFirebase認証を用意。
    private val auth = FirebaseAuth.getInstance()

   // 回答データを保存する「クラウド上のデータベース（Firestore）」を用意。
    private val firestore = FirebaseFirestore.getInstance()


    //_uiState: 今の画面の状態（入力中の内容・エラー・投稿中など）を保存する変数。
    //
    //uiState: 外から読み取る専用（中身は変えられない）。
    private val _uiState = MutableStateFlow(AnswerSendUiState())
    val uiState: StateFlow<AnswerSendUiState> = _uiState.asStateFlow()


    //ユーザーが入力した回答文を「今の状態」に保存する関数。
    fun updateBody(body: String) {
        _uiState.value = _uiState.value.copy(body = body)
    }


    //エラーメッセージを消す関数（エラーが出た後のリセット用）。
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }


    //回答を送信するときに呼び出される関数。
    fun submitAnswer(context: Context, questionUid: String, genreId: Int) {

       //currentState は「今の画面の状態（入力内容など）」を一時的に取り出す。
        val currentState = _uiState.value

        // バリデーション
        //回答が空欄なら、「入力してください」とエラーを出して終わり。
        if (currentState.body.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "回答内容を入力してください")
            return
        }
//回答が長すぎたらエラーを出す（1000文字までOK）。
        if (currentState.body.length > 1000) {
            _uiState.value = currentState.copy(errorMessage = "回答内容は1000文字以内で入力してください")
            return
        }



        //ログインしていない人は回答できない。
        //→ ログインしていなければエラーを出す。
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = currentState.copy(errorMessage = "ログインが必要です")
            return
        }


        //ここから「投稿中の状態」に切り替える。
        //（ロード中のくるくるマークが出るイメージ）
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {//時間のかかる処理（通信）を安全に実行するスタート。
            try {
                // ユーザーの表示名を取得(端末の中に保存されている「ユーザー名（ニックネーム）」を取り出す。)
                val displayName = getDisplayNameFromDataStore(context)

                // 回答データを作成
                val answer = Answer(
                    body = currentState.body,
                    name = displayName,
                    uid = currentUser.uid
                )

                // Firestoreに回答を保存

                //Firebaseの中で「どの質問に対しての回答か」を指し示す道を作る。
                val questionRef = firestore
                    .collection(Const.ContentsPATH)
                    .document(genreId.toString())
                    .collection("questions")
                    .document(questionUid)


                //その質問の中に新しい回答を入れるための場所を作る。
                //document() は新しいIDを自動で作ってくれる。
                val answerRef = questionRef
                    .collection(Const.AnswersPATH)
                    .document()

                // answerUidを設定)(今作った回答に、その自動生成されたIDをセット。)
                val answerWithUid = answer.copy(answerUid = answerRef.id)

                // Firestoreに保存
                answerRef.set(answerWithUid).await()


                //「投稿が成功した！」という状態に切り替える。
                _uiState.value = currentState.copy(
                    isLoading = false,
                    isAnswerPosted = true
                )

            } catch (e: Exception) {//通信に失敗したりしたときに、エラーを表示する。
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "回答の投稿に失敗しました: ${e.message}"
                )
            }
        }
    }
}