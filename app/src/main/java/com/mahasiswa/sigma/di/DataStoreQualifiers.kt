package com.mahasiswa.sigma.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DisasterReportsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VolunteerDataStore
