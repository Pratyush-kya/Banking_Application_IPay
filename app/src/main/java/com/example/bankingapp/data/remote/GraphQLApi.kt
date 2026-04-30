package com.example.bankingapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GraphQLApi {
    @POST("graphql")
    suspend fun executeMutation(
        @Body request: GraphQLRequest
    ): Response<GraphQLResponse<SendMoneyData>>
}
