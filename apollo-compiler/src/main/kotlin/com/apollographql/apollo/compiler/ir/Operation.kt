package com.apollographql.apollo.compiler.ir

data class Operation(
    val operationName: String,
    val operationType: String,
    val variables: List<Variable>,
    val source: String,
    val fields: List<Field>,
    val filePath: String,
    val fragmentsReferenced: List<String>,
    val operationId: String
) {

  fun normalizedOperationName(useSemanticNaming: Boolean): String = when (operationType) {
    TYPE_MUTATION -> normalizedOperationName(useSemanticNaming, "Mutation")
    TYPE_QUERY -> normalizedOperationName(useSemanticNaming, "Query")
    TYPE_SUBSCRIPTION -> normalizedOperationName(useSemanticNaming, "Subscription")
    else -> throw IllegalArgumentException("Unknown operation type $operationType")
  }

  private fun normalizedOperationName(useSemanticNaming: Boolean, operationNameSuffix: String): String {
    return if (useSemanticNaming && !operationName.endsWith(operationNameSuffix)) {
      operationName.capitalize() + operationNameSuffix
    } else {
      operationName.capitalize()
    }
  }

  fun isMutation() = operationType == TYPE_MUTATION

  fun isQuery() = operationType == TYPE_QUERY

  fun isSubscription() = operationType == TYPE_SUBSCRIPTION

  companion object {
    const val DATA_TYPE_NAME = "Data"
    const val TYPE_MUTATION = "mutation"
    const val TYPE_QUERY = "query"
    const val TYPE_SUBSCRIPTION = "subscription"
  }
}
