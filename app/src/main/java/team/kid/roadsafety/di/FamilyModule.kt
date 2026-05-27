package team.kid.roadsafety.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import team.kid.roadsafety.data.repository.FamilyRepositoryImpl
import team.kid.roadsafety.domain.aggregates.family.FamilyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FamilyModule {

    @Binds
    @Singleton
    abstract fun bindFamilyRepository(
        familyRepositoryImpl: FamilyRepositoryImpl
    ): FamilyRepository
}
