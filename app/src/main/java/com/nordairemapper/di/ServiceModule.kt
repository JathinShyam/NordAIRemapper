package com.nordairemapper.di

import com.nordairemapper.service.ActionDispatcher
import com.nordairemapper.service.RemapActionExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    abstract fun bindActionDispatcher(impl: RemapActionExecutor): ActionDispatcher
}
