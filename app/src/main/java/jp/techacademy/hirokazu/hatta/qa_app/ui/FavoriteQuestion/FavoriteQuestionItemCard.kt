package jp.techacademy.hirokazu.hatta.qa_app.ui.FavoriteQuestion

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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import android.util.Base64
import java.text.SimpleDateFormat
import java.util.*

@Composable//@Composable は「画面に表示できる関数」という意味
fun FavoriteQuestionItemCard(//お気に入り質問”1つ"を表示するカードの部品 を作る関数です。
    favoriteQuestion: Question,//表示する質問データ。
    modifier: Modifier = Modifier//modifier は表示の幅や高さなどを柔軟に変えるための箱（省略可）。
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                // 質問タイトル
                Text(
                    text = favoriteQuestion.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,//maxLines = 1 で1行だけ表示、長いと「…」で省略。
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 投稿者名と投稿日時
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "投稿者: ${favoriteQuestion.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 投稿日時
                    if (favoriteQuestion.timestamp > 0) {
                        val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())//SimpleDateFormat で「月/日 時:分」の形に整形。
                        Text(
                            text = dateFormat.format(Date(favoriteQuestion.timestamp)),
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
                            text = "回答 ${favoriteQuestion.answerCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 右側の画像サムネイル（あれば）
            favoriteQuestion.imageString?.let { imageString ->
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