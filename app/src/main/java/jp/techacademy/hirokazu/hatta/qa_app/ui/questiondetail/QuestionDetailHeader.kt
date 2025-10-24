package jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import jp.techacademy.hirokazu.hatta.qa_app.data.Question
import jp.techacademy.hirokazu.hatta.qa_app.data.User
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuestionDetailHeader(
    question: Question,
    viewModel: QuestionDetailViewModel,
    user: User,//お気に入り登録機能に必要
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 質問タイトル
            Text(
                text = question.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            //お気に入り登録機能
            IconButton(
                onClick = { viewModel.toggleFavorite(question,user) },//クリックしたらviewModel.toggleFavoriteを呼ぶ
                modifier = Modifier.size(40.dp)
            ) {
                Icon(//questionのfavoritedByマップのuser.uidにタイする値に、よってハートマークの色を変える
                    imageVector = if (question.favoritedBy[user.uid] == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (question.favoritedBy[user.uid] == true) "Remove from favorites" else "Add to favorites",
                    tint = if (question.favoritedBy[user.uid] == true) Color.Red else Color.Gray
                )
            }
        }

            // 投稿者情報と投稿日時
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "投稿者: ${question.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (question.timestamp > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(question.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 添付画像（あれば）
            question.imageString?.let { imageString ->
                if (imageString.isNotBlank()) {
                    val imageBytes = try {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } catch (e: Exception) {
                        android.util.Log.w("QuestionDetailHeader", "画像のデコードに失敗: ${e.message}")
                        null
                    }
                    if (imageBytes != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageBytes)
                                .build(),
                            contentDescription = "添付画像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // 質問本文
            Text(
                text = question.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
