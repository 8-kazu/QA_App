package jp.techacademy.hirokazu.hatta.qa_app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import jp.techacademy.hirokazu.hatta.qa_app.data.User
import jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail.QuestionDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable//@Composable は「画面に表示できる関数」という意味
fun QuestionItemCard(//QuestionItemCard は 質問1つを表示するカードの部品 を作る関数です。
    question: Question,//question は表示する質問データ。
    onClick: () -> Unit,//onClick はカードを押したときの処理を外から渡すための箱。
    viewModel: MainViewModel,
    user: User,
    modifier: Modifier = Modifier//modifier は表示の幅や高さなどを柔軟に変えるための箱（省略可）。
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },//clickable { onClick() } でカードを押したら onClick を実行。onClickは後で引数として渡す
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)//elevation はカードの影の深さ。
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側の質問情報
            Column(
                modifier = Modifier.weight(1f)//weight(1f) は「横幅の残り全部を使う」という意味。
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // 質問タイトル
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,//maxLines = 1 で1行だけ表示、長いと「…」で省略。
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                IconButton(

                    onClick = { viewModel.toggleFavorite(question,user) },//


                    modifier = Modifier.size(40.dp)

                ) {


                    Icon(

                        imageVector = if (question.favoritedBy[user.uid] == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (question.favoritedBy[user.uid] == true) "Remove from favorites" else "Add to favorites",
                        tint = if (question.favoritedBy[user.uid] == true) Color.Red else Color.Gray
                    )
                }}

                // 投稿者名と投稿日時
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "投稿者: ${question.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 投稿日時
                    if (question.timestamp > 0) {
                        val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())//SimpleDateFormat で「月/日 時:分」の形に整形。
                        Text(
                            text = dateFormat.format(Date(question.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 回答数
                    Surface(
                        //背景を色付きにして角を丸く
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "回答 ${question.answerCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 右側の画像サムネイル（あれば）
            question.imageString?.let { imageString ->
                if (imageString.isNotBlank()) {//imageString が空でなければ処理を続ける。
                    Spacer(modifier = Modifier.width(8.dp))

                    //文字列として保存されている画像を バイトデータに変換。
                    //
                    //失敗したら無視（nullにする）。
                    val imageBytes = try {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } catch (e: Exception) {
                        null
                    }

                    imageBytes?.let { bytes ->//変換できたら 画像を表示。
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(bytes)
                                .build(),
                            contentDescription = "質問画像",
                            contentScale = ContentScale.Crop,//ContentScale.Crop で枠に合わせて切り取り表示。
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))//サイズは56dp四方で角を丸くする。
                        )
                    }
                }
            }
        }
    }
}