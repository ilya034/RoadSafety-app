package team.kid.roadsafety.data.remote

import retrofit2.Response
import retrofit2.http.*
import team.kid.roadsafety.data.dto.*

interface RoadSafetyApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponseDto>

    @POST("families")
    suspend fun createFamily(@Body request: FamilyCreateRequestDto): Response<FamilyResponseDto>

    @POST("families/invite-code")
    suspend fun createInviteCode(@Body request: CreateInviteCodeRequestDto): Response<InviteCodeResponseDto>

    @POST("families/join-by-invite")
    suspend fun joinFamily(@Body request: JoinFamilyByInviteCodeRequestDto): Response<JoinFamilyByInviteCodeResponseDto>

    @GET("families/{familyId}")
    suspend fun getFamily(@Path("familyId") familyId: String): Response<FamilyResponseDto>

    @GET("families/{familyId}/members")
    suspend fun getFamilyMembers(@Path("familyId") familyId: String): Response<GetFamilyMembersResponseDto>

    @GET("map/areas")
    suspend fun getAreas(@Query("cityId") cityId: String?): Response<List<MapAreaResponseDto>>

    @GET("families/{familyId}/map/areas")
    suspend fun getUserAreas(
        @Path("familyId") familyId: String,
        @Query("childId") childId: String?
    ): Response<List<MapAreaResponseDto>>

    @PATCH("map/areas/{areaId}/color")
    suspend fun updateAreaColor(
        @Path("areaId") areaId: String,
        @Body request: UpdateAreaColorRequestDto
    ): Response<Unit>
}
