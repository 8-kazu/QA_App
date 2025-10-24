package jp.techacademy.hirokazu.hatta.qa_app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.techacademy.hirokazu.hatta.qa_app.ui.login.LoginScreen
import jp.techacademy.hirokazu.hatta.qa_app.ui.main.MainScreen

import androidx.navigation.NavType
import androidx.navigation.navArgument

import jp.techacademy.hirokazu.hatta.qa_app.ui.questionsend.QuestionSendScreen

import jp.techacademy.hirokazu.hatta.qa_app.ui.settings.SettingScreen

import jp.techacademy.hirokazu.hatta.qa_app.ui.questiondetail.QuestionDetailScreen

import jp.techacademy.hirokazu.hatta.qa_app.ui.answersend.AnswerSendScreen

import jp.techacademy.hirokazu.hatta.qa_app.ui.FavoriteQuestion.FavoriteScreen


@Composable
fun QAAppNavHost(//アプリ全体の画面遷移をまとめて管理する関数を定義。
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route//ログイン画面からスタート
    //startDestination はアプリ起動時に最初に表示する画面。
) {
    NavHost(//NavHost は「どの画面がどのタイミングで表示されるか」をまとめる場所。
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        //ログイン画面の処理
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {//ログインがうまくいったら、、、
                    navController.navigate(Screen.Main.route) {//navController.navigate(...)は別画面に行きたいときに使う
                        //Screen.Main.routeは、Screen クラスの中に定義された、Main画面を表すオブジェクトの持つルート文字列」 という意味
                        // ログイン画面を履歴から削除
                        popUpTo(Screen.Login.route) { inclusive = true }//履歴からログイン画面を消して、戻るボタンを押しても戻れないようにする
                   //popUpTo() は「どこまでスタックを消すか」を指定するもの。
                        //inclusive = true は、「その指定した画面 自身も削除する」という意味。
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {// ログアウトが成功したら、、、、
                    navController.navigate(Screen.Login.route) {//Screen クラスの中に定義された、Login画面を表すオブジェクトの持つルート文字列が住所になっている画面に移動する
                        // すべての画面を履歴から削除
                        popUpTo(0) { inclusive = true }
                    }
                },

                //設定画面、質問投稿画面、質問詳細画面、お気に入り画面へ遷移する処理を指定。
                onNavigateToSettings = {
                    navController.navigate(Screen.Setting.route)
                },
                onNavigateToQuestionSend = { genreId ->
                    navController.navigate(Screen.QuestionSend.createRoute(genreId))
                },
                onNavigateToQuestionDetail = { questionUid, genreId ->
                    navController.navigate(Screen.QuestionDetail.createRoute(questionUid, genreId))
                },
                onNavigateToFavoriteQuestion = {
                    navController.navigate(Screen.Favorites.route)
                }
            )
        }

// 将来の実装用のプレースホルダー
        composable(//composable() は「画面（ページ）を作るためのブロック」。
            route = Screen.QuestionSend.route,
            arguments = listOf(navArgument("genreId") { type = NavType.IntType })
        ) { backStackEntry ->//backStackEntry は「この画面に来たときの情報（引数）」が入っている。
            val genreId = backStackEntry.arguments?.getInt("genreId") ?: 1//ここで「ジャンルID（数値）」を取り出している。
            //もし何も送られてこなかったら、デフォルト（初期値）として「1（趣味など）」を使う。

            QuestionSendScreen(
                genreId = genreId,//ジャンルIDを渡して画面を表示。

                //onNavigateBack = { navController.popBackStack() }
                //　→ 「戻るボタンを押したとき」に前の画面に戻るようにしている。
                onNavigateBack = {
                    navController.popBackStack()//戻るときは popBackStack() で前の画面に戻る。
                }
            )
        }

        composable(//質問詳細画面の処理
            route = Screen.QuestionDetail.route,
            arguments = listOf(
                navArgument("questionUid") { type = NavType.StringType },
                navArgument("genreId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val questionUid = backStackEntry.arguments?.getString("questionUid") ?: ""
            val genreId = backStackEntry.arguments?.getInt("genreId") ?: 1
            QuestionDetailScreen(
                questionUid = questionUid,
                genreId = genreId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAnswerSend = { questionUid, genreId ->
                    navController.navigate(Screen.AnswerSend.createRoute(questionUid, genreId))
                }
            )
        }

        composable(
            route = Screen.AnswerSend.route,
            arguments = listOf(
                navArgument("questionUid") { type = NavType.StringType },
                navArgument("genreId") { type = NavType.IntType }
            )
        ) {
            // AnswerSendScreen(...)
                backStackEntry ->
            val questionUid = backStackEntry.arguments?.getString("questionUid") ?: ""
            val genreId = backStackEntry.arguments?.getInt("genreId") ?: 1
            AnswerSendScreen(
                questionUid = questionUid,
                genreId = genreId,
                onNavigateBack = {
                    navController.popBackStack()
                })
        }

        composable(Screen.Setting.route) {
            // SettingScreen(...)
            SettingScreen(
                onNavigateBack = {
                    navController.popBackStack()//戻るときは前の画面に戻る。
                },
                onLogout = {//ログアウトすると全画面を消してログイン画面へ。
                    navController.navigate(Screen.Login.route) {
                        // すべての画面を履歴から削除
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

//お気に入り一覧画面に行く
        composable(Screen.Favorites.route) {
            FavoriteScreen(
                onNavigateBack = {
                    navController.popBackStack()//戻るときは前の画面に戻る。
                }
            )
        }


    }
}