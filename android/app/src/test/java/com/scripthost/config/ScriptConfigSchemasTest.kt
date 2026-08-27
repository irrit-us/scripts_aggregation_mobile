package com.scripthost.config

import com.google.common.truth.Truth.assertThat
import com.scripthost.util.ConsoleLogger
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for [ScriptConfigSchemas]: JSON validation, persistence
 * round-trip, and per-script removal. Pure JVM (TemporaryFolder only), so
 * [ConsoleLogger] is used everywhere instead of the Android default.
 */
class ScriptConfigSchemasTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var schemas: ScriptConfigSchemas

    @Before
    fun setUp() {
        schemas = ScriptConfigSchemas(tempFolder.root, ConsoleLogger())
    }

    private fun parse(json: String) =
        ScriptConfigSchemas.parseFields(json, ConsoleLogger())

    private fun validSchemaJson() = """
        [
          { "key": "OPENAI_API_KEY", "label": "API Key", "type": "password" },
          { "key": "AGENT_API_URL", "label": "API Base URL", "type": "text",
            "default": "https://api.openai.com/v1" },
          { "key": "AGENT_MODEL", "label": "Model", "type": "select",
            "options": ["gpt-4o-mini", "gpt-4o"], "default": "gpt-4o-mini" },
          { "key": "AGENT_STREAM", "label": "Stream", "type": "boolean",
            "default": false }
        ]
    """.trimIndent()

    @Test
    fun putThenGet_roundTripsAcrossInstances() {
        val fields = parse(validSchemaJson())!!
        schemas.put("script-a", "Agent Chat", fields)

        val reloaded = ScriptConfigSchemas(tempFolder.root, ConsoleLogger())
        val schema = reloaded.get("script-a")

        assertThat(schema).isNotNull()
        assertThat(schema!!.name).isEqualTo("Agent Chat")
        assertThat(schema.fields).hasSize(4)
        assertThat(schema.fields[0]).isEqualTo(
            ScriptConfigSchemas.Field("OPENAI_API_KEY", "API Key", "password")
        )
        assertThat(schema.fields[2].options).containsExactly("gpt-4o-mini", "gpt-4o").inOrder()
        assertThat(schema.fields[2].default).isEqualTo("gpt-4o-mini")
        assertThat(schema.fields[3].default).isEqualTo("false")
    }

    @Test
    fun put_replacesPreviousDeclaration() {
        val fields = parse(validSchemaJson())!!
        schemas.put("script-a", "Old Name", fields)
        schemas.put("script-a", "New Name", fields.take(1))

        val schema = schemas.get("script-a")!!
        assertThat(schema.name).isEqualTo("New Name")
        assertThat(schema.fields).hasSize(1)
    }

    @Test
    fun parseFields_malformedJson_returnsNull() {
        assertThat(parse("not json")).isNull()
        assertThat(parse("{\"obj\": true}")).isNull()
    }

    @Test
    fun parseFields_skipsInvalidFieldsAndKeepsValidOnes() {
        val fields = parse(
            """
            [
              { "key": "GOOD", "label": "Good", "type": "text" },
              { "key": "BAD_TYPE", "type": "color" },
              { "label": "no key", "type": "text" },
              { "key": "BAD_SELECT", "type": "select" },
              { "key": "GOOD_BOOL", "type": "boolean", "default": true },
              "not-an-object"
            ]
            """.trimIndent()
        )!!

        assertThat(fields.map { it.key }).containsExactly("GOOD", "GOOD_BOOL").inOrder()
        assertThat(fields[1].default).isEqualTo("true")
    }

    @Test
    fun parseFields_skipsDuplicateKeys() {
        val fields = parse(
            """
            [
              { "key": "DUP", "type": "text" },
              { "key": "DUP", "type": "password" }
            ]
            """.trimIndent()
        )!!

        assertThat(fields).hasSize(1)
        assertThat(fields[0].type).isEqualTo("text")
    }

    @Test
    fun parseFields_labelDefaultsToKey() {
        val fields = parse("""[ { "key": "NO_LABEL", "type": "number" } ]""")!!

        assertThat(fields[0].label).isEqualTo("NO_LABEL")
    }

    @Test
    fun parseFields_acceptsMultilineType() {
        val fields = parse(
            """[ { "key": "PLAN_MD", "label": "Plan", "type": "multiline",
                  "default": "# Warm-up" } ]"""
        )!!

        assertThat(fields).hasSize(1)
        assertThat(fields[0].type).isEqualTo("multiline")
        assertThat(fields[0].default).isEqualTo("# Warm-up")
    }

    @Test
    fun removeScript_dropsEntry() {
        val fields = parse(validSchemaJson())!!
        schemas.put("script-a", "Agent Chat", fields)
        schemas.put("script-b", "Other", fields)

        schemas.removeScript("script-a")

        assertThat(schemas.get("script-a")).isNull()
        assertThat(schemas.get("script-b")).isNotNull()
        val reloaded = ScriptConfigSchemas(tempFolder.root, ConsoleLogger())
        assertThat(reloaded.get("script-a")).isNull()
    }

    @Test
    fun removeScript_unknownId_isNoOp() {
        schemas.removeScript("never-declared")
        assertThat(schemas.all()).isEmpty()
    }
}
