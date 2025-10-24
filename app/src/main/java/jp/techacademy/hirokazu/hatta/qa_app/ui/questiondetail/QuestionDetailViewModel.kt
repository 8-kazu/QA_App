package jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

//Firebaseの「認証」と「データベース（Firestore）」を使うため。
import com.google.firebase.firestore.FirebaseFirestore


import com.google.firebase.firestore.ListenerRegistration
import jp.techacademy.hirokazu.hatta.qa_app.Const
import jp.techacademy.hirokazu.hatta.qa_app.data.Answer
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import jp.techacademy.hirokazu.hatta.qa_app.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil

class QuestionDetailViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()//Firebaseの「データベース」を使うための準備。


    //_uiStateは中身を変える用、uiStateは見る専用。
    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()


    //Firebaseの「リアルタイム更新」を監視するためのリスナー（耳）を用意。
    private var questionListener: ListenerRegistration? = null
    private var answersListener: ListenerRegistration? = null

    fun loadQuestionDetail(questionUid: String, genreId: Int) {//「特定の質問（UIDで指定）と、そのジャンルのデータを読み込む」関数。
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)//「読み込み中だよ〜」という状態にする。

        // 質問詳細を取得
        loadQuestion(questionUid, genreId)

        // 回答一覧をリアルタイムで取得
        loadAnswers(questionUid, genreId)
    }


    //「安全に（アプリが落ちないように）質問を取ってくる」処理を始める。
    private fun loadQuestion(questionUid: String, genreId: Int) {
        viewModelScope.launch {
            try {
                //Firebaseのデータベースから「指定ジャンルの中の指定質問」を1つ取ってくる。
                val document = firestore.collection(Const.ContentsPATH)
                    .document(genreId.toString())
                    .collection("questions")
                    .document(questionUid)
                    .get()
                    .await()


                //Firebaseから取ってきたデータを「Question」というデータ形式に変換する。
                val question = document.toObject(Question::class.java)?.copy(
                    questionUid = document.id
                )

                if (question != null) {
                    //取れたら、画面の状態を「質問をセット＆読み込み完了」に変える。
                    _uiState.value = _uiState.value.copy(question = question, isLoading = false)
                } else {//見つからなかった場合はエラーメッセージを表示。
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "質問が見つかりません"
                    )
                }
            } catch (e: Exception) {//通信エラーなどのときにエラーメッセージをセット。
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "質問の取得に失敗しました: ${e.message}"
                )
            }
        }
    }

    private fun loadAnswers(questionUid: String, genreId: Int) {
        // 既存のリスナーがあれば解除
        answersListener?.remove()

        // 回答一覧をリアルタイムで取得
        answersListener = firestore.collection(Const.ContentsPATH)
            .document(genreId.toString())
            .collection("questions")
            .document(questionUid)
            .collection(Const.AnswersPATH)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {//エラーが起きたときは画面にエラーメッセージを出す。
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "回答の取得に失敗しました: ${e.message}"
                    )
                    return@addSnapshotListener
                }


                //Firebaseの中の「回答一覧」をひとつずつAnswerクラスに変換してリスト化。
                val answers = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject(Answer::class.java)?.copy(
                            answerUid = document.id
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("QuestionDetailViewModel", "回答データの変換に失敗: ${e.message}")
                        null
                    }
                } ?: emptyList()


                //取ってきた回答一覧を画面の状態に反映する。
                _uiState.value = _uiState.value.copy(answers = answers)
            }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }


    //画面が閉じられたときに、Firebaseの監視をやめて無駄な通信を止める。
    override fun onCleared() {
        super.onCleared()
        questionListener?.remove()
        answersListener?.remove()
    }



    /**
     * お気に入り状態を切り替え
     * @param question 対象
     */
    fun toggleFavorite(
        question: Question,
        user: User) {
        viewModelScope.launch {
            val newStatus = DataStoreUtil.toggleFavorite(question, user)//今のユーザーに対するお気に入り状態を反対にしたのをutilから返してもらって受け取る

            // favoritedByを更新する（「Map（マップ）」 は、「キー（名前）」と「値（データ）」をセットで保存するコレクション（入れ物）」）
            val updatedFavoritedBy = question.favoritedBy.toMutableMap()//questionのfavoritedByを「読み取り専用（変更できない）」 Map から変更可能な MutableMap に変換する
            updatedFavoritedBy[user.uid] = newStatus // ← ここでMapを更新（今のユーザーに対する、クエスチョンのお気に入り状態を更新）

            // questionを更新し、uiStateに反映（見た目を変えるためのコード）
            _uiState.value = _uiState.value.copy(
                question = question.copy(favoritedBy = updatedFavoritedBy)//favoritedByを新しいfavoritedByに更新する
            )
        }
}}