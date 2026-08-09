package com.nordairemapper.di

import com.nordairemapper.service.ActionDispatcher
import com.nordairemapper.service.LogOnlyActionDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    abstract fun bindActionDispatcher(impl: LogOnlyActionDispatcher): ActionDispatcher
}
