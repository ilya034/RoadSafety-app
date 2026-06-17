package team.kid.roadsafety.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import team.kid.roadsafety.BuildConfig
import team.kid.roadsafety.data.remote.RoadSafetyApi
import team.kid.roadsafety.infrastructure.AuthInterceptor
import team.kid.roadsafety.infrastructure.TokenAuthenticator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val debugLevel = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        val debugHeadersLevel = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE

        val bodyLogger = HttpLoggingInterceptor().apply { level = debugLevel }
        val headersLogger = HttpLoggingInterceptor().apply { level = debugHeadersLevel }

        val safeLoggingInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            if (path.contains("/maps/alert-zones") || path.endsWith(".pbf")) {
                headersLogger.intercept(chain)
            } else {
                bodyLogger.intercept(chain)
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(safeLoggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRoadSafetyApi(retrofit: Retrofit): RoadSafetyApi {
        return retrofit.create(RoadSafetyApi::class.java)
    }
}
