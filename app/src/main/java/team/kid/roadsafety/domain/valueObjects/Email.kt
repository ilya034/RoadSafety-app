package team.kid.roadsafety.domain.valueObjects

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        fun create(input: String): Email {
            require(isValid(input)) { "Invalid email address: $input" }
            return Email(input)
        }

        fun isValid(email: String): Boolean {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }
}