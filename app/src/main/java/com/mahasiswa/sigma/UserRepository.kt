package com.mahasiswa.sigma

object UserRepository {
    private val users = mutableListOf<User>(
        // Pre-created BNPB account as per requirements
        User("Admin BNPB", "admin@bnpb.go.id", "admin123", "BNPB")
    )

    fun addUser(user: User): Boolean {
        // BNPB users cannot be added via registration
        if (user.role == "BNPB") return false

        if (users.any { it.email == user.email }) return false
        users.add(user)
        return true
    }

    fun login(email: String, password: String, role: String): User? {
        return users.find { it.email == email && it.password == password && it.role == role }
    }
}
