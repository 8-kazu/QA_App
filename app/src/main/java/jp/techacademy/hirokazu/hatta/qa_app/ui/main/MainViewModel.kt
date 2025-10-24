package jp.techacademy.hirokazu.hatta.qa_app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import jp.techacademy.hirokazu.hatta.qa_app.Const
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import jp.techacademy.hirokazu.hatta.qa_app.data.User
import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil
import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.getDisplayNameFromDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(//「データクラス」と呼ばれるもので、アプリ画面の状態をまとめて入れる箱を作っています。
    val currentUser: User = User(),//「currentUser」は「今ログインしているユーザーの情報」です。 「User()」で、空のユーザー（初期状態）を入れています。
    val selectedGenre: Int = Const.GENRE_HOBBY,
    //selectedGenre は 今選ばれているジャンル（カテゴリ）の番号 を持つ箱。
    //
    //型は Int（整数）。
    //
    //Const.GENRE_HOBBY は「趣味ジャンル」の番号を表す定数で、最初は趣味が選ばれている状態です。
    val questions: List<Question> = emptyList(),
    //questions は 画面に表示する質問のリスト です。
    //
    //型は List<Question> で、「Question という型のものが複数入るリスト」です。
    //
    //= emptyList() で最初は何も質問が入っていない状態になります。
    val isDrawerOpen: Boolean = false,
    //isDrawerOpen は 左側のメニュー（ドロワー）が開いているかどうか を表す箱。
    //
    //型は Boolean（true/false）で、最初は閉じているので false です。
    val isLoading: Boolean = false,//isLoading は 画面がデータを読み込んでいるかどうか を表します。
    val errorMessage: String? = null,
    //errorMessage は エラーが起きたときのメッセージ を入れる箱。
    //
    //型は String? で、「文字列か、何もない（null）か」のどちらかです。

    val question: Question
)

class MainViewModel : ViewModel() {
    //MainViewModel という 画面の状態を管理する箱（クラス） を作っています。
    //
    //ViewModel を継承しているので、画面のデータを安全に保持でき、画面回転などでも消えません。
    private val auth = FirebaseAuth.getInstance()
    //auth は Firebaseの認証機能 を使うための箱です。
    //
    //ここでログインしているユーザー情報を取ったり、ログイン操作をしたりできます。
    private val firestore = FirebaseFirestore.getInstance()
    //private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MainUiState(question = Question()))//Question 型の変数を「空の初期値」で作る という意味
    //_uiState は 画面の今の状態を保存する箱 です。
    //
    //MutableStateFlow は「変化を監視できる箱」で、中身が変わると自動で画面に通知されます。
    //
    //最初は MainUiState() で初期状態を作っています。
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
//uiState は 外から見るための箱 です。
//
//_uiState は書き換え可能ですが、uiState は読むだけで書き換えはできません。
//
//「画面はこれを見て状態を更新する」イメージです。
    private var questionsListener: ListenerRegistration? = null
    //questionsListener は 質問データをリアルタイムで監視するための登録情報 です。
    //
    //監視をやめたいときにこの情報を使って解除します。
    private val answerListeners = mutableMapOf<String, ListenerRegistration>()
//answerListeners は 各質問ごとの回答監視の登録情報 をまとめた地図（Map）です。
//
//質問IDをキーにして、監視登録情報を保存します。
    fun loadUserInfo(context: Context) {//loadUserInfo は 今ログインしているユーザーの情報を画面に読み込む関数。
        viewModelScope.launch {//「非同期で処理する」ときに使います（処理中に画面が止まらない）。
            try {
                val currentUser = auth.currentUser//auth.currentUser で今ログイン中のユーザー情報を取得。
                if (currentUser != null) {
                    // DataStoreから表示名を取得
                    val displayName = getDisplayNameFromDataStore(context)
                    _uiState.value = _uiState.value.copy(
                        currentUser = User(uid = currentUser.uid, displayName = displayName)
                    )//getDisplayNameFromDataStore で名前を取得して、_uiState に保存。
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "ユーザー情報の取得に失敗しました: ${e.message}"//エラーが出たら、errorMessage にエラー内容を入れます
                )
            }
        }
    }

    fun selectGenre(genreId: Int) {
        _uiState.value = _uiState.value.copy(selectedGenre = genreId)
        loadQuestions(genreId)//selectedGenre を新しいジャンルIDに更新し、loadQuestions でそのジャンルの質問を読み込みます。
    }



    //refreshQuestions は 今のジャンルの質問をもう一度読み込む 関数です。
    fun refreshQuestions() {
        loadQuestions(_uiState.value.selectedGenre)
    }



    //toggleDrawer は メニューの開閉を切り替える。
    //
    //! は「反対にする」という意味で、開いていれば閉じる、閉じていれば開く。
    fun toggleDrawer() {
        _uiState.value = _uiState.value.copy(isDrawerOpen = !_uiState.value.isDrawerOpen)
    }


    //closeDrawer は メニューを閉じるだけ の関数。
    fun closeDrawer() {
        _uiState.value = _uiState.value.copy(isDrawerOpen = false)
    }


    //loadQuestions は 指定したジャンルの質問をFirestoreから読み込む関数。
    private fun loadQuestions(genreId: Int) {
        // 既存のリスナーを解除
        questionsListener?.remove()
        clearAnswerListeners()

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        // Firestoreからリアルタイムで質問一覧を取得（作成日時の降順でソート）
        questionsListener = firestore.collection(Const.ContentsPATH)
            .document(genreId.toString())
            .collection("questions")
            .orderBy("timestamp", Query.Direction.DESCENDING)//作成日時順（新しい順）で取得。
            .addSnapshotListener { snapshot, e ->
                if (e != null) {//エラーがあったら errorMessage に入れて isLoading を false にします。
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "質問の取得に失敗しました: ${e.message}"
                    )
                    return@addSnapshotListener
                }

                val questions = snapshot?.documents?.mapNotNull { document ->
                    try {
                        val question = document.toObject(Question::class.java)?.copy(
                            questionUid = document.id
                        )
                        // 質問の必須フィールドをバリデーション
                        question?.takeIf {
                            it.title.isNotBlank() &&
                                    it.body.isNotBlank() &&
                                    it.name.isNotBlank() &&
                                    it.uid.isNotBlank()
                        }?.let {
                            // 回答数を取得してquestionオブジェクトを更新
                            loadAnswerCount(genreId, it)
                        }
                        question?.takeIf {
                            it.title.isNotBlank() &&
                                    it.body.isNotBlank() &&
                                    it.name.isNotBlank() &&
                                    it.uid.isNotBlank()
                        }
                    } catch (e: Exception) {//失敗したらログに出して無視。
                        // ログ出力（本来はFirebase Crashlyticsなどを使用）
                        android.util.Log.w("MainViewModel", "質問データの変換に失敗: ${e.message}")
                        null
                    }
                } ?: emptyList()



                //変換した質問リストを _uiState に入れ、読み込み終了。
                _uiState.value = _uiState.value.copy(
                    questions = questions,
                    isLoading = false
                )
            }
    }

    //各質問の 回答数をFirestoreから取得して更新 する関数。
    private fun loadAnswerCount(genreId: Int, question: Question) {//各質問の 回答数をFirestoreから取得して更新 する関数。
        // 既存のリスナーがあれば解除
        answerListeners[question.questionUid]?.remove()

        // 各質問の回答数をリアルタイムで取得
        val listener = firestore.collection(Const.ContentsPATH)
            .document(genreId.toString())
            .collection("questions")
            .document(question.questionUid)
            .collection(Const.AnswersPATH)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    val answerCount = snapshot.size()
                    // 質問リストの該当質問の回答数を更新
                    val currentQuestions = _uiState.value.questions
                    val updatedQuestions = currentQuestions.map { q ->
                        if (q.questionUid == question.questionUid) {
                            q.copy(answerCount = answerCount)
                        } else {
                            q
                        }
                    }
                    _uiState.value = _uiState.value.copy(questions = updatedQuestions)
                }
            }

        answerListeners[question.questionUid] = listener
    }

    //既存の回答監視を解除して、Mapを空にする。
    private fun clearAnswerListeners() {
        answerListeners.values.forEach { it.remove() }
        answerListeners.clear()
    }


    //ジャンルIDを 文字列に変換 して画面表示用に返す関数。
    fun getGenreName(genreId: Int): String {
        return when (genreId) {
            Const.GENRE_HOBBY -> "趣味"
            Const.GENRE_LIFE -> "生活"
            Const.GENRE_HEALTH -> "健康"
            Const.GENRE_COMPUTER -> "コンピューター"
            else -> "すべて"
        }
    }


    //エラー表示を消す関数。
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }


    //ViewModelが破棄されるときに 監視を解除してメモリ解放。
    override fun onCleared() {
        super.onCleared()
        questionsListener?.remove()
        clearAnswerListeners()
    }



    /**
     * お気に入り状態を切り替え
     * @param question 対象
     */
    fun toggleFavorite(
        question: Question,
                       user: User) {
        viewModelScope.launch {
            // FavoriteManagerを使用

            //val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false


            val newStatus = DataStoreUtil.toggleFavorite(question, user)
            // newStatusがtrueならお気に入り登録済み

            // favoritedByを更新する
            val updatedFavoritedBy = question.favoritedBy.toMutableMap()
            updatedFavoritedBy[user.uid] = newStatus // ← ここでMapを更新

            // questionを更新し、uiStateに反映
            _uiState.value = _uiState.value.copy(
                question = question.copy(favoritedBy = updatedFavoritedBy)
            )
            //_uiState.value = _uiState.value.copy(question.favoritedBy[question.questionUid] = newStatus)

        }
    }



}