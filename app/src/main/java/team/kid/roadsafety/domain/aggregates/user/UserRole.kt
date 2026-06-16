package team.kid.roadsafety.domain.aggregates.user

enum class UserRole {
    PARENT,
    CHILD;

    companion object {
        fun fromString(value: String?): UserRole? {
            if (value == null) return null
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                when (value.lowercase()) {
                    "parent" -> PARENT
                    "child" -> CHILD
                    else -> null
                }
            }
        }
    }
}
