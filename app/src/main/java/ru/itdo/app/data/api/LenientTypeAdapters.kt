package ru.itdo.app.data.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Бэкенд (PHP) не гарантирует стабильный JSON-тип для булевых и числовых
 * полей: где-то true/false, где-то 0/1, где-то "0"/"1" строкой (см. те же
 * поля is_group/is_nuksta/read и т.п., из-за которых на iOS-клиенте
 * пришлось писать ручные decode-фолбэки в Models.swift — там прямо
 * закомментировано "иначе весь список падал"). Gson по умолчанию строгий:
 * несовпадение типа -> JsonSyntaxException на всём ответе. Эти адаптеры
 * повторяют защитную логику iOS-клиента на стороне Android.
 */
object LenientBooleanTypeAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            JsonToken.NULL -> { reader.nextNull(); false }
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NUMBER -> reader.nextDouble() != 0.0
            JsonToken.STRING -> {
                val s = reader.nextString().trim()
                s == "1" || s.equals("true", ignoreCase = true)
            }
            else -> { reader.skipValue(); false }
        }
    }
}

/** Аналогично: сервер иногда шлёт числовые поля строкой ("likes_count": "3"). */
object LenientIntTypeAdapter : TypeAdapter<Int>() {
    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonToken.NULL -> { reader.nextNull(); 0 }
            JsonToken.NUMBER, JsonToken.STRING -> {
                val raw = reader.nextString()
                raw.toDoubleOrNull()?.toInt() ?: 0
            }
            JsonToken.BOOLEAN -> if (reader.nextBoolean()) 1 else 0
            else -> { reader.skipValue(); 0 }
        }
    }
}

/** Аналогично для Long (created_at иногда unix timestamp числом, иногда строкой). */
object LenientLongTypeAdapter : TypeAdapter<Long>() {
    override fun write(out: JsonWriter, value: Long?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): Long {
        return when (reader.peek()) {
            JsonToken.NULL -> { reader.nextNull(); 0L }
            JsonToken.NUMBER, JsonToken.STRING -> {
                val raw = reader.nextString()
                raw.toDoubleOrNull()?.toLong() ?: 0L
            }
            else -> { reader.skipValue(); 0L }
        }
    }
}
