package team.kid.roadsafety.infrastructure

import org.json.JSONObject
import retrofit2.Response

fun Response<*>.parseErrorMessage(defaultMessage: String): String {
    var errorMessage = "$defaultMessage: ${this.code()}"
    try {
        val errorBodyString = this.errorBody()?.string()
        if (!errorBodyString.isNullOrBlank()) {
            val json = JSONObject(errorBodyString)
            val title = json.optString("title", "")
            val detail = json.optString("detail", "")
            
            val parts = mutableListOf<String>()
            if (title.isNotBlank()) parts.add(title)
            if (detail.isNotBlank()) parts.add(detail)

            if (this.code() == 400 && json.has("errors")) {
                val errorsObj = json.optJSONObject("errors")
                if (errorsObj != null) {
                    val keys = errorsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val errorArray = errorsObj.optJSONArray(key)
                        if (errorArray != null) {
                            for (i in 0 until errorArray.length()) {
                                val errorMsg = errorArray.optString(i)
                                if (errorMsg.isNotBlank()) {
                                    parts.add("- $key: $errorMsg")
                                }
                            }
                        }
                    }
                }
            }

            if (parts.isNotEmpty()) {
                errorMessage = parts.joinToString("\n")
            }
        }
    } catch (e: Exception) {
        // Ignore JSON parsing errors
    }
    return errorMessage
}
