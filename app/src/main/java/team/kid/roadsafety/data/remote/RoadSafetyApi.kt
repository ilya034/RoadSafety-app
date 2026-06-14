package team.kid.roadsafety.data.remote

import retrofit2.Response
import retrofit2.http.*
import team.kid.roadsafety.data.dto.*

interface RoadSafetyApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("auth/refresh")
    suspend fun refreshTokens(@Body request: RefreshTokensRequestDto): Response<RefreshTokensResponseDto>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogOutRequestDto): Response<Unit>

    @GET("users")
    suspend fun getUserByContact(
        @Query("email") email: String? = null,
        @Query("phone") phone: String? = null
    ): Response<UserResponseDto>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponseDto>

    @POST("families")
    suspend fun createFamily(@Body request: CreateFamilyRequestDto): Response<CreateFamilyResponseDto>

    @PUT("families/{familyId}/city")
    suspend fun updateFamilyCity(
        @Path("familyId") familyId: String,
        @Body request: UpdateFamilyCityRequestDto
    ): Response<Unit>

    @GET("families/{familyId}/members")
    suspend fun getFamilyMembers(@Path("familyId") familyId: String): Response<GetFamilyMembersResponseDto>

    @POST("families/invite-code")
    suspend fun createInviteCode(@Body request: CreateInviteCodeRequestDto): Response<InviteCodeResponseDto>

    @POST("families/join-by-invite")
    suspend fun joinFamily(@Body request: JoinFamilyByInviteCodeRequestDto): Response<JoinFamilyByInviteCodeResponseDto>

    @GET("maps/cities")
    suspend fun getMapCities(): Response<MapCitiesResponseDto>

    @GET("maps/cities/{cityId}/metadata")
    suspend fun getCityMetadata(@Path("cityId") cityId: String): Response<MapCityMetadataDto>

    @GET("maps/user-areas")
    suspend fun getUserAreas(
        @Query("familyId") familyId: String,
        @Query("childId") childId: String? = null
    ): Response<UserMapAreaFeatureCollectionDto>

    @POST("maps/user-areas/base-overrides")
    suspend fun createBaseAreaOverride(
        @Body request: CreateBaseAreaOverrideRequestDto
    ): Response<UserMapAreaFeatureDto>

    @POST("maps/user-areas/custom")
    suspend fun createCustomUserMapArea(
        @Body request: CreateCustomUserMapAreaRequestDto
    ): Response<UserMapAreaFeatureDto>

    @POST("tracking/location")
    suspend fun submitChildLocation(@Body request: SubmitLocationRequestDto): Response<SubmitLocationResponseDto>

    @GET("tracking/children/{childId}/location")
    suspend fun getChildLocation(@Path("childId") childId: String): Response<ChildLocationResponseDto>

    @GET("tracking/children/{childId}/stats")
    suspend fun getChildStats(@Path("childId") childId: String): Response<ChildStatsResponseDto>
}
