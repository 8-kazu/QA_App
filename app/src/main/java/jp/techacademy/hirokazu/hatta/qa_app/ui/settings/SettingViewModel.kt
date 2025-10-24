package jp.techacademy.hirokazu.hatta.qa_app.ui.settings

import android.content.Context//Androidの「アプリ全体の情報（例：データ保存場所）」にアクセスするための道具。
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope//viewModelScopeは「このViewModelの中で安全に非同期処理（時間がかかる処理）をするための枠」。

//GoogleのFirebaseというサービスを使うため。
//FirebaseAuthは「ログイン情報」、FirebaseFirestoreは「データベース」。
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import jp.techacademy.hirokazu.hatta.qa_app.Const//定数（固定の文字や数字）をまとめたファイルを読み込む。


//ユーザー名をスマホ内部の「DataStore（小さな保存箱）」から読み込んだり、保存したりする関数。
import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.getDisplayNameFromDataStore
import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.saveDisplayNameToDataStore

//これらはすべて「非同期処理（時間のかかる処理）」や「リアルタイムで画面を更新する」ための機能
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class SettingViewModel : ViewModel(){//設定画面専用のViewModel。

    //Firebaseのログイン機能とデータベース機能を使えるように準備。
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // UI State(_uiStateが中身で、uiStateが外から見える部分。)(画面とViewModelをつなぐ橋)
    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()


    //表示名をスマホの中（DataStore）から読み込む関数。
    fun loadDisplayName(context: Context) {
        viewModelScope.launch {//非同期処理のスタート。「時間がかかってもアプリが止まらないように」する。
            try {
                //スマホの中から、保存してある「表示名（ニックネーム）」を取り出す。
                val displayName = getDisplayNameFromDataStore(context)

                //今の画面状態に「取り出した名前」を反映。
                _uiState.value = _uiState.value.copy(displayName = displayName)
            } catch (e: Exception) {//もし失敗したら（データが壊れてるなど）、例外処理をする。
                _uiState.value = _uiState.value.copy(//copy() の意味は、データを「不変（イミュータブル）」として扱うために、既存のオブジェクトを基に一部のプロパティだけを変更した「新しいオブジェクト」を生成すること
                    errorMessage = "表示名の読み込みに失敗しました: ${e.message}"
                )
            }
        }
    }


    //ユーザーが入力した新しい名前を一時的に画面状態に保存しておく。
    //→ エラー表示は消す。
    fun updateDisplayName(displayName: String) {
        _uiState.value = _uiState.value.copy(displayName = displayName, errorMessage = null)
    }


    //実際に「名前を変更」する処理のスタート。
    fun changeDisplayName(context: Context) {
        //現在の状態（名前、エラー、読み込み中かどうか）を取得。
        val currentState = _uiState.value


        //名前が空っぽなら「入力してください」とエラーを出して処理を中断。
        if (currentState.displayName.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "表示名を入力してください")
            return
        }

        if (currentState.displayName.length > 50) {//名前が長すぎたらエラー。
            _uiState.value = currentState.copy(errorMessage = "表示名は50文字以内で入力してください")
            return
        }


        //今ログインしていなかったら「ログインが必要」と出して終了。
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = currentState.copy(errorMessage = "ログインが必要です")
            return
        }


        //「いま変更中です」と表示する準備。
        //→ 画面でぐるぐるマーク（ローディング）を出すときに使う。
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // Firestoreに表示名を保存
                firestore.collection(Const.UsersPATH)
                    .document(currentUser.uid)
                    .update(Const.NameKEY, currentState.displayName)
                    .await()

                // DataStoreに表示名を保存(同じ名前をスマホの中（DataStore）にも保存しておく。)
                saveDisplayNameToDataStore(context, currentState.displayName)

                _uiState.value = currentState.copy(//処理が成功したら「変更しました」と画面に知らせる。
                    isLoading = false,
                    successMessage = "表示名を変更しました"
                )
            } catch (e: Exception) {//失敗した場合はエラーメッセージを表示。
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "表示名の変更に失敗しました: ${e.message}"
                )
            }
        }
    }


    //ログアウト処理をする関数。
    fun logout() {
        //「いまログアウト中」と状態を更新。
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {

                //Firebaseのログイン情報を削除してログアウト。
                auth.signOut()
                //成功したら「ログアウト完了」と知らせる。
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedOut = true
                )
            } catch (e: Exception) {//失敗したらエラーを出す。
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "ログアウトに失敗しました: ${e.message}"
                )
            }
        }
    }






    //エラーや成功メッセージをリセット（画面に残らないようにする）関数。
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}