package team.kid.roadsafety.domain.valueObjects

@JvmInline
value class PhoneNumber private constructor(val value: String) {
    companion object {
        fun create(input: String): PhoneNumber {
            require(input.length == 12) { "Invalid phone number" }
            return PhoneNumber(input)
        }
    }
}