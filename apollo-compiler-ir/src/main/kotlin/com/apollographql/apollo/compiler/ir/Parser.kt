@file:Suppress("unused")

package com.apollographql.apollo.compiler.ir

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File

object Parser {
  private val adapter: JsonAdapter<CodeGenerationIR> = Moshi.Builder().build().adapter(CodeGenerationIR::class.java)

  fun parse(irFile: File): CodeGenerationIR = adapter.fromJson(irFile.readText())!!
}
