package jp.techacademy.hirokazu.hatta.qa_app.ui.questionsend

import android.content.Context//Androidアプリの「今のアプリの状況」や「資源」にアクセスするための型（Context）を使えるようにする宣言。
import android.graphics.Bitmap//画像（ビットマップ）を扱うための型を使えるようにする宣言
import android.graphics.BitmapFactory//画像ファイルをBitmapに変換するための道具を使えるようにする宣言。
import android.net.Uri
import android.util.Base64//バイナリ（画像データなど）を文字列（Base64）に変換する機能を使えるようにする宣言。
import androidx.lifecycle.ViewModel//Androidの設計パターンで「画面の状態（データ）を管理する担当（ViewModel）」を使うための宣言。
import androidx.lifecycle.viewModelScope//ViewModelの中で非同期（時間のかかる処理）を安全に行うための「範囲」を使えるようにする宣言。
import com.google.firebase.auth.FirebaseAuth//Firebase（認証サービス）を使って「今ログインしているユーザー」を扱うための宣言
import com.google.firebase.firestore.FirebaseFirestore//import com.google.firebase.firestore.FirebaseFirestore
import jp.techacademy.hirokazu.hatta.qa_app.Const//アプリ内で共通に使う定数（パス名など）が書かれたファイルを使えるようにする宣言
import jp.techacademy.hirokazu.hatta.qa_app.data.Question//「Question」というデータの形（質問データの構造）を使えるようにする宣言。
import jp.techacademy.hirokazu.hatta.qa_app.util.DataStoreUtil.getDisplayNameFromDataStore//保存してある表示名（ニックネーム）を取り出すための補助関数を使えるようにする宣言。
import kotlinx.coroutines.flow.MutableStateFlow//画面の状態を流れるデータとして管理するための「変更可能な箱」を使えるようにする宣言。
import kotlinx.coroutines.flow.StateFlow//画面の状態を外部に読み取り専用で渡すための型を使えるようにする宣言。
import kotlinx.coroutines.flow.asStateFlow//MutableStateFlowを外から書き換えられないStateFlowに変換するための道具を使えるようにする宣言。
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await//Firebaseなどの「非同期で終わる処理」が終わるのを待つための書き方を使えるようにする宣言。
import java.io.ByteArrayOutputStream//画像データを一時的にバイト（0/1の並び）としてためる箱を使えるようにする宣言。
import java.io.InputStream//ファイルの中身を読み出すための入力ストリーム型を使えるようにする宣言。




//「質問投稿画面のデータと処理」をまとめるクラス（設計図）を作っています。ViewModelを継承しているので画面の状態管理に向いています。
class QuestionSendViewModel : ViewModel(){
    private val auth = FirebaseAuth.getInstance()//Firebase認証サービスを使うために、操作オブジェクトを作っています。（ログイン情報を取り出すときに使う）
    private val firestore = FirebaseFirestore.getInstance()//Firestore（クラウドデータベース）にアクセスするためのオブジェクトを作っています。

    private val _uiState = MutableStateFlow(QuestionSendUiState())//画面の現在の状態（タイトルや本文、画像URI、エラーなど）を入れておく「変更可能な箱」を作っています。初期値は空の QuestionSendUiState()。
    val uiState: StateFlow<QuestionSendUiState> = _uiState.asStateFlow()//外側（UI側）には読み取り専用の形で状態を渡します。UIはこれを見て表示を更新しますが直接は書き換えられません。

    fun initialize(genreId: Int) {//初期化用の関数（ジャンルをセットするための処理）を定義しています。
        //現在の状態を複製して、ジャンルだけ genreId に変えて保存しています。（StateFlowの値を更新）
        _uiState.value = _uiState.value.copy(selectedGenre = genreId)
    }

    //タイトルが入力されたときに呼ばれる関数の始まり。
    fun updateTitle(title: String) {
        //タイトルを更新して、同時に以前のエラー表示を消しています（エラーが出てたらリセット）。
        _uiState.value = _uiState.value.copy(title = title, errorMessage = null)
    }


    //本文が入力されたときに呼ばれる関数の始まり。
    fun updateBody(body: String) {
        _uiState.value = _uiState.value.copy(body = body, errorMessage = null)
    }

    //ユーザーがジャンルを選んだときに呼ばれる関数の始まり。
    fun updateSelectedGenre(genreId: Int) {
        _uiState.value = _uiState.value.copy(selectedGenre = genreId)
    }


//画像を選んだ（または選択を解除した）ときに呼ばれる関数の始まり。Uri? の ? は「画像がないこともある」という意味。
    fun updateSelectedImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
    }


    //エラー表示を消したいときに呼ぶ関数の始まり。
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }


    //「質問を送信する」処理を始める関数の宣言。context はユーザーの表示名を取るのに使います。
    fun submitQuestion(context: Context) {

        //今の画面の状態を currentState という変数にコピーして扱いやすくします
        val currentState = _uiState.value

        // バリデーション
        if (currentState.title.isBlank()) {
            //空ならエラーメッセージをセットして…
            _uiState.value = currentState.copy(errorMessage = "タイトルを入力してください")
            //停止。
            return
        }

        if (currentState.title.length > 100) {
            //長すぎたらエラーメッセージを出して…
            _uiState.value = currentState.copy(errorMessage = "タイトルは100文字以内で入力してください")
            return
        }

        if (currentState.body.isBlank()) {
            //本文が空白かどうかチェックしています。
            _uiState.value = currentState.copy(errorMessage = "質問内容を入力してください")
            return
        }


        //本文が長すぎないか（1000文字超え）チェックしています。
        if (currentState.body.length > 1000) {
            //   //長すぎたらエラーを出して…
            _uiState.value = currentState.copy(errorMessage = "質問内容は1000文字以内で入力してください")
            return
        }


        //ジャンルが正しく選ばれているか（番号が0以下なら未選択扱い）チェックしています。
        if (currentState.selectedGenre <= 0) {
//未選択ならエラーメッセージを出して…
            _uiState.value = currentState.copy(errorMessage = "ジャンルが選択されていません")
            return
        }

        //今ログインしているユーザー情報を取り出します（いなければ null）。
        val currentUser = auth.currentUser
        //ログインしていないかどうかをチェック。
        if (currentUser == null) {
            //ログインしていなければエラーをセットして…
            _uiState.value = currentState.copy(errorMessage = "ログインが必要です")
            return
        }
//ここまでのチェックを通ったら「送信中フラグ」を立てて（ロード中表示）、エラーを消します。
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {//ここから「例外（エラー）が起きても安全に処理する」ブロックを始めます。
                // ユーザーの表示名を取得
                val displayName = getDisplayNameFromDataStore(context)

                // 画像をBase64に変換（画像が選択されている場合）
                val imageString = currentState.selectedImageUri?.let { uri ->
                    encodeImageToBase64(context, uri)
                }

                // 質問データを作成
                val question = Question(
                    title = currentState.title,
                    body = currentState.body,
                    name = displayName,
                    uid = currentUser.uid,
                    genre = currentState.selectedGenre,
                    imageString = imageString,
                    timestamp = System.currentTimeMillis()
                )

                // Firestoreに保存
                firestore.collection(Const.ContentsPATH)//Firestoreの「トップのコレクション（グループ）」を指定しています（Constで定義されたパス）。
                    .document(currentState.selectedGenre.toString())//その中の「ジャンル別ドキュメント」を選んでいます（ジャンル番号を文字列にして使う
                    .collection("questions")//さらにその中の「questions」というコレクション（質問の一覧）を選んでいます。
                    .add(question)//先ほど作った question をデータベースに追加します。
                    .await()//非同期処理が終わるのを待ちます（保存が完了するまでここで待機）

                _uiState.value = currentState.copy(//保存が成功したら UI の状態を更新します。
                    isLoading = false,// isLoading = false,
                    isQuestionPosted = true//投稿が完了したことを示すフラグを true にします。
                )

            } catch (e: Exception) {//もし途中で何か例外（エラー）が起きたらここでキャッチします。
                _uiState.value = currentState.copy(//エラーが発生したときは UI 状態をエラー表示に更新します。
                    isLoading = false,
                    errorMessage = "質問の投稿に失敗しました: ${e.message}"//エラーメッセージをユーザーに見せるためにセットします（何が起きたかの概要を表示）
                )
            }
        }
    }


    //画像を読み込んで Base64 文字列に変換する関数の宣言。suspend は非同期で呼ぶ関数という意味。
    private suspend fun encodeImageToBase64(context: Context, uri: Uri): String? {
        return try {//成功したら文字列を返す、失敗したら null を返すという形の開始
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)//画像ファイルの中身を読み出すためのストリームを開きます。
            val bitmap = BitmapFactory.decodeStream(inputStream)//開いたストリームから画像データをBitmap（扱いやすい画像型）に変換します。
            inputStream?.close()//開いたストリームを閉じてリソースを解放します（安全対策）。

            // 画像をリサイズ（大きすぎる場合）
            val resizedBitmap = resizeBitmap(bitmap, 1024, 1024)

            // Bitmapを Base64 文字列に変換
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()//出来上がったバイト列を配列として取り出します。

            Base64.encodeToString(byteArray, Base64.DEFAULT)//バイト列をBase64文字列に変換して返します（これで文字列として保存・送信可能）。
        } catch (e: Exception) {//失敗したら null を返す
            null
        }
    }


    //画像を指定された最大幅・高さに収めるための縮小処理の関数を宣言。
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width//元画像の幅を取得
        val height = bitmap.height//元画像の高さを取得。

        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        return if (scale < 1.0f) {//scale が 1 未満（縮小が必要）なら縮小処理をする、そうでなければ元のまま返すという分岐。
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {//そうでなければ元のまま返す
            bitmap
        }
    }

}