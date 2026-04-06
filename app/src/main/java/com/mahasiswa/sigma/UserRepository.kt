package com.mahasiswa.sigma

object UserRepository {
    private val users = mutableListOf<User>()

    fun addUser(user: User): Boolean {
        if (users.any { it.email == user.email }) return false
        users.add(user)
        return true
    }

    fun login(email: String, password: String, role: String): User? {
        return users.find { it.email == email && it.password == password && it.role == role }
    }
}
