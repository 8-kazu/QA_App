package jp.techacademy.hirokazu.hatta.qa_app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")

    object Favorites : Screen("favorite")



    object QuestionSend : Screen("question_send/{genreId}") {
        fun createRoute(genreId: Int) = "question_send/$genreId"
    }
    object QuestionDetail : Screen("question_detail/{questionUid}/{genreId}") {
        fun createRoute(questionUid: String, genreId: Int) = "question_detail/$questionUid/$genreId"
    }
    object AnswerSend : Screen("answer_send/{questionUid}/{genreId}") {
        fun createRoute(questionUid: String, genreId: Int) = "answer_send/$questionUid/$genreId"
    }
    object Setting : Screen("setting")
}