package com.example.bankingapp.data.remote

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any>
)

data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>?
)

data class GraphQLError(
    val message: String
)

data class SendMoneyData(
    val user_update: List<Any>?,
    val transaction_insert: Any?
)
