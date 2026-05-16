package team.kid.roadsafety.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import team.kid.roadsafety.data.dto.*

interface RoadSafetyApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("families")
    suspend fun createFamily(@Body request: FamilyCreateRequestDto): Response<FamilyResponseDto>

    @POST("families/join")
    suspend fun joinFamily(@Body request: FamilyJoinRequestDto): Response<FamilyMemberResponseDto>

    @POST("families/{familyId}/invites")
    suspend fun createInviteCode(@Path("familyId") familyId: String): Response<InviteCodeResponseDto>

    @GET("families/{familyId}")
    suspend fun getFamily(@Path("familyId") familyId: String): Response<FamilyResponseDto>

    @GET("families/{familyId}/members")
    suspend fun getFamilyMembers(@Path("familyId") familyId: String): Response<List<FamilyMemberResponseDto>>
}
