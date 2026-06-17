package team.kid.roadsafety.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import team.kid.roadsafety.data.dto.AuthResponseDto
import team.kid.roadsafety.data.dto.ChildLocationResponseDto
import team.kid.roadsafety.data.dto.ChildLocationsResponseDto
import team.kid.roadsafety.data.dto.ChildStatsResponseDto
import team.kid.roadsafety.data.dto.CreateBaseAreaOverrideRequestDto
import team.kid.roadsafety.data.dto.CreateCustomUserMapAreaRequestDto
import team.kid.roadsafety.data.dto.CreateFamilyRequestDto
import team.kid.roadsafety.data.dto.CreateFamilyResponseDto
import team.kid.roadsafety.data.dto.CreateInviteCodeRequestDto
import team.kid.roadsafety.data.dto.GetFamilyMembersResponseDto
import team.kid.roadsafety.data.dto.InviteCodeResponseDto
import team.kid.roadsafety.data.dto.JoinFamilyByInviteCodeRequestDto
import team.kid.roadsafety.data.dto.JoinFamilyByInviteCodeResponseDto
import team.kid.roadsafety.data.dto.LogOutRequestDto
import team.kid.roadsafety.data.dto.LoginRequestDto
import team.kid.roadsafety.data.dto.MapCitiesResponseDto
import team.kid.roadsafety.data.dto.MapCityMetadataDto
import team.kid.roadsafety.data.dto.NotificationsResponseDto
import team.kid.roadsafety.data.dto.RefreshTokensRequestDto
import team.kid.roadsafety.data.dto.RefreshTokensResponseDto
import team.kid.roadsafety.data.dto.RegisterRequestDto
import team.kid.roadsafety.data.dto.SubmitLocationRequestDto
import team.kid.roadsafety.data.dto.SubmitLocationResponseDto
import team.kid.roadsafety.data.dto.UpdateFamilyCityRequestDto
import team.kid.roadsafety.data.dto.UserMapAreaFeatureCollectionDto
import team.kid.roadsafety.data.dto.UserMapAreaFeatureDto
import team.kid.roadsafety.data.dto.UserResponseDto

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

    @GET("maps/alert-zones")
    suspend fun getAlertZones(
        @Query("cityId") cityId: String,
        @Query("familyId") familyId: String,
        @Query("childId") childId: String? = null
    ): Response<ResponseBody>

    @POST("maps/user-areas/base-overrides")
    suspend fun createBaseAreaOverride(
        @Body request: CreateBaseAreaOverrideRequestDto
    ): Response<UserMapAreaFeatureDto>

    @POST("maps/user-areas/custom")
    suspend fun createCustomUserMapArea(
        @Body request: CreateCustomUserMapAreaRequestDto
    ): Response<UserMapAreaFeatureDto>

    @DELETE("maps/user-areas/custom/{areaId}")
    suspend fun deleteCustomArea(
        @Path("areaId") areaId: String
    ): Response<Unit>

    @DELETE("maps/user-areas/base-overrides")
    suspend fun resetBaseAreaColor(
        @Query("familyId") familyId: String,
        @Query("baseAreaKey") baseAreaKey: String,
        @Query("childId") childId: String? = null
    ): Response<Unit>

    @POST("tracking/location")
    suspend fun submitChildLocation(@Body request: SubmitLocationRequestDto): Response<SubmitLocationResponseDto>

    @GET("tracking/children/{childId}/location")
    suspend fun getChildLocation(@Path("childId") childId: String): Response<ChildLocationResponseDto>

    @GET("tracking/children/locations")
    suspend fun getChildrenLocations(): Response<ChildLocationsResponseDto>

    @GET("tracking/children/{childId}/stats")
    suspend fun getChildStats(@Path("childId") childId: String): Response<ChildStatsResponseDto>

    @GET("notifications")
    suspend fun getNotifications(@Query("unreadOnly") unreadOnly: Boolean = false): Response<NotificationsResponseDto>

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>
}
