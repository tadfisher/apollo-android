package com.apollographql.apollo.compiler.ir

data class Fragment(
    val fragmentName: String,
    val source: String,
    val typeCondition: String,
    val possibleTypes: List<String>,
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val fragmentsReferenced: List<String>
) {
  fun formatClassName() = fragmentName.capitalize()
}
