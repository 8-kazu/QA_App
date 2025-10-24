package jp.techacademy.hirokazu.hatta.qa_app.ui.questionsend


import android.net.Uri//スマホの中の写真やファイルの場所をあらわす「住所（URI）」を使えるようにするための準備。
data class QuestionSendUiState(//「質問を送る画面の状態」をまとめて管理するための設計図を作っている。
    val title: String = "",//「質問のタイトル」を入れておく箱。最初は空っぽ。
    val body: String = "",//「質問の本文」を入れる箱。これも最初は空っぽ。
    val selectedGenre: Int = 0,//「どのジャンルの質問か」を数字で管理するためのもの。
    val selectedImageUri: Uri? = null,
    //質問に「画像」を添付したいとき、その画像の場所（住所）を入れる箱。
    //「?」がついてるのは、「画像を選ばない（＝何も入っていない）」こともあるから。
    val isLoading: Boolean = false,//「今、送信中です！」みたいな状態を表すスイッチ。
    val errorMessage: String? = null,//もしエラー（送信失敗など）があったら、その「エラーメッセージ」を入れる場所。
    val isQuestionPosted: Boolean = false//「質問がちゃんと投稿できたかどうか」を表すスイッチ。
)
