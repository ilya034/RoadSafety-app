package team.kid.roadsafety.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import team.kid.roadsafety.data.repository.TrackingRepositoryImpl
import team.kid.roadsafety.domain.aggregates.tracking.TrackingRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(
        trackingRepositoryImpl: TrackingRepositoryImpl
    ): TrackingRepository
}
