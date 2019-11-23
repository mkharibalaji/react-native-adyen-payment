package com.rnlib.adyen

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType

import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException

object RNUtils {

      fun writableMapOf(vararg values: Pair<String, *>): WritableMap {
        val map = Arguments.createMap()
        for ((key, value) in values) {
            when (value) {
                null -> map.putNull(key)
                is Boolean -> map.putBoolean(key, value)
                is Double -> map.putDouble(key, value)
                is Int -> map.putInt(key, value)
                is String -> map.putString(key, value)
                is WritableMap -> map.putMap(key, value)
                is WritableArray -> map.putArray(key, value)
                else -> throw IllegalArgumentException("Unsupported value type ${value::class.java.name} for key [$key]")
            }
        }
        return map
    }

    fun writableArrayOf(vararg values: Any?): WritableArray {
        val array = Arguments.createArray()
        for (value in values) {
            when (value) {
                null -> array.pushNull()
                is Boolean -> array.pushBoolean(value)
                is Double -> array.pushDouble(value)
                is Int -> array.pushInt(value)
                is String -> array.pushString(value)
                is WritableArray -> array.pushArray(value)
                is WritableMap -> array.pushMap(value)
                else -> throw IllegalArgumentException("Unsupported type ${value::class.java.name}")
            }
        }
        return array
    }
    
  fun convertJsonToMap(jsonObject:JSONObject):WritableMap {
    val map = Arguments.createMap()
    try{
      val iterator = jsonObject.keys()
      while (iterator.hasNext())
      {
        val key = iterator.next()
        val value = jsonObject.get(key)
        when (value) {
            null -> map.putNull(key)
            is JSONObject -> map.putMap(key, convertJsonToMap(value))
            is JSONArray -> map.putArray(key, convertJsonToArray(value))
            is Boolean -> map.putBoolean(key, value)
            is Double -> map.putDouble(key, value)
            is Int -> map.putInt(key, value)
            is String -> map.putString(key, value)
            is WritableMap -> map.putMap(key, value)
            is WritableArray -> map.putArray(key, value)
            else -> map.putString(key, value.toString())
        }
      }
    }catch(e: Exception){
      throw JSONException("JSON Parsing Error")
    }
    return map
  }


  fun convertJsonToArray(jsonArray:JSONArray):WritableArray {
    val array = Arguments.createArray()
      for (i in 0 until jsonArray.length()) {
          val value = jsonArray.get(i)
          when (value) {
              null -> array.pushNull()
              is JSONObject -> array.pushMap(convertJsonToMap(value))
              is JSONArray -> array.pushArray(convertJsonToArray(value))
              is Boolean -> array.pushBoolean(value)
              is Double -> array.pushDouble(value)
              is Int -> array.pushInt(value)
              is String -> array.pushString(value)
              is WritableArray -> array.pushArray(value)
              is WritableMap -> array.pushMap(value)
              else -> array.pushString(value.toString())
          }
      }
    return array
  }

  /*

  fun convertMapToJson(readableMap:ReadableMap):JSONObject {
    val json_obj = JSONObject()
    val iterator = readableMap.keySetIterator()
    while (iterator.hasNextKey())
    {
      val key = iterator.nextKey()
      when (readableMap.getType(key)) {
        ReadableType.Null -> json_obj.put(key, JSONObject.NULL)
        ReadableType.Boolean -> json_obj.put(key, readableMap.getBoolean(key))
        ReadableType.Number -> json_obj.put(key, readableMap.getDouble(key))
        ReadableType.String -> json_obj.put(key, readableMap.getString(key))
        ReadableType.Map -> json_obj.put(key, RNUtils.convertMapToJson(readableMap.getMap(key)))
        ReadableType.Array -> json_obj.put(key, convertArrayToJson(readableMap.getArray(key)))
      }
    }
    return json_obj
  }


  fun convertArrayToJson(readableArray:ReadableArray):JSONArray {
    val array = JSONArray()
    for (i in 0 until readableArray.size())
    {
      when (readableArray.getType(i)) {
        ReadableType.Null -> {}
        ReadableType.Boolean -> array.put(readableArray.getBoolean(i))
        ReadableType.Number -> array.put(readableArray.getDouble(i))
        ReadableType.String -> array.put(readableArray.getString(i))
        ReadableType.Map -> array.put(convertMapToJson(readableArray.getMap(i)))
        ReadableType.Array -> array.put(convertArrayToJson(readableArray.getArray(i)))
      }
    }
    return array
  }*/

}