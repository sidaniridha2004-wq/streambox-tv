package com.streambox.tv.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * All dependencies in this module are constructor-injected via @Inject, so
 * this module is currently empty. Kept as a single place to add bindings if
 * we later need to swap implementations (e.g. fake repository in tests).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
