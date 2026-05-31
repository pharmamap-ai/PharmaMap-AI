package com.example.pharmamapapp.appwrite

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
)
