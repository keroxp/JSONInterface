package me.keroxp.lib.jsoninterface;

import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract "Interface" class for instantiating Data Model Object from JSON String
 * Created by keroxp on 2014/12/22.
 */

public abstract class JSONInterface {

    /**
     * snake_case to camelCase
     * http://blog.kengo-toda.jp/entry/20081130/1228026182
     */
    protected static String snakeToCamel(String targetStr) {
        Pattern p = Pattern.compile("_([a-z])");
        Matcher m = p.matcher(targetStr.toLowerCase());
        StringBuffer sb = new StringBuffer(targetStr.length());
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * camelCase to snake_case
     * @param targetStr
     * @return
     */
    protected static String camelToSnake(String targetStr) {
        String convertedStr = targetStr
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                .replaceAll("([a-z])([A-Z])", "$1_$2");
        return convertedStr.toLowerCase();
    }

    /**
     * mapping JSON property's key and Java class field name
     * @param attr
     * @return
     */
    public String getJavaFieldName (String attr) {
        // By default, snake_case -> camelCase
        return snakeToCamel(attr);
    }

    /**
     * mapping Java field name and JSON field property's key
     * @param field
     * @return
     */
    public String getJSONFieldName (String field) {
        // By defalt, camelCase -> snake_case
        return camelToSnake(field);
    }

    /**
     * Instantiate JSONInterface Object by Reflection
     * @param clazz
     * @param json
     * @return
     * @throws Exception
     */
    public static JSONInterface instantiate (Class clazz, JSONObject json) throws Exception {
        Iterator<String> it = json.keys();
        JSONInterface instance = (JSONInterface) clazz.newInstance();
        while(it.hasNext()) {
            String key = it.next();
            String fieldName = instance.getJavaFieldName(key);
            Class self = instance.getClass();
            Object val = null;
            try {
                // following statement may cause IllegalAccessException if
                // java-class doesn't have mapped-field
                Field field = self.getDeclaredField(fieldName);
                // make it accessible if the field is protected or private.
                // this behavior is based on concept that Mapped-Java Class
                // perfectly reflects JSON's structure.
                field.setAccessible(true);
                Class fieldType = field.getType();
                if (JSONInterface.class.isAssignableFrom(fieldType)) {
                    // If given class is sub-class of JSONInterface,
                    // then instantiate recursively
                    JSONObject json_ = json.getJSONObject(key);
                    JSONInterface child = instantiate(fieldType, json_);
                    field.set(instance, child);
                }else if (ArrayList.class.isAssignableFrom(fieldType)) {
                    // If of Generic-Typed Array, instantiate GT Array and
                    // append values.
                    JSONArray jsonArray = json.getJSONArray(key);
                    int count = jsonArray.length();
                    ParameterizedType genericType = (ParameterizedType)field.getGenericType();
                    Class genericClass = (Class)genericType.getActualTypeArguments()[0];
                    ArrayList arrayList = ArrayList.class.getConstructor(int.class).newInstance(count);
                    if (JSONInterface.class.isAssignableFrom(genericClass)) {
                        JSONObject json_;
                        JSONInterface item;
                        for (int i = 0; i < count; ++i) {
                            json_ = jsonArray.getJSONObject(i);
                            item = instantiate(genericClass, json_);
                            arrayList.add(item);
                        }
                    }else {
                        Object item;
                        for (int i = 0; i< count; ++i) {
                            item = getValue(genericClass, jsonArray, i);
                            arrayList.add(item);
                        }
                    }
                    val = arrayList;
                } else {
                    val = getValue(fieldType, json, key);
                }
                if (val != null) {
                    field.set(instance, val);
                }else{
                    // TODO: You can customize a behavior when json field is null
                    field.set(instance,val);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    /**
     * Serialize JSONInterface into JSON
     * @param writer
     * @param jsonInterface
     * @throws Exception
     */
    public static void serializeToJSON (JsonWriter writer, JSONInterface jsonInterface) throws Exception {
        mSerializeToJSON(writer,jsonInterface,true);
    }
    private static void mSerializeToJSON (JsonWriter writer, JSONInterface jsonInterface, Boolean isRoot) throws Exception {
        if (isRoot) {
            writer.beginObject();
        }
        Field[] fields = jsonInterface.getClass().getDeclaredFields();
        for (int i = 0, max = fields.length; i < max; ++i) {
            Field field = fields[i];
            field.setAccessible(true);
            Class clazz = field.getType();
            String jsonFieldName = jsonInterface.getJSONFieldName(field.getName());
            if (JSONInterface.class.isAssignableFrom(clazz)) {
                // JSONInterface object
                JSONInterface j = (JSONInterface) field.get(jsonInterface);
                writer.name(jsonFieldName);
                if (j != null) {
                    writer.beginObject();
                    mSerializeToJSON(writer, j, false);
                    writer.endObject();
                }else{
                    writer.nullValue();
                }
            } else if (ArrayList.class.isAssignableFrom(clazz)) {
                // An Array of JSONInterface
                ArrayList arrayList = (ArrayList) field.get(jsonInterface);
                Iterator it = arrayList.iterator();
                writer.name(jsonFieldName);
                if (arrayList != null) {
                    writer.beginArray();
                    ParameterizedType genericType = (ParameterizedType)field.getGenericType();
                    Class genericClass = (Class)genericType.getActualTypeArguments()[0];
                    while (it.hasNext()) {
                        Object val = it.next();
                        if (val == null) {
                            writer.nullValue();
                        }else if (JSONInterface.class.isAssignableFrom(genericClass)) {
                            mSerializeToJSON(writer, (JSONInterface) val, true);
                        }else {
                            writeValue(writer, field, val);
                        }
                    }
                    writer.endArray();
                } else {
                    writer.nullValue();
                }
            } else {
                writer.name(jsonFieldName);
                // ordinal value
                Object val = field.get(jsonInterface);
                writeValue(writer, field, val);
            }
        }
        if (isRoot) {
            writer.endObject();
        }
    }

    private static void writeValue(JsonWriter writer, Field field, Object val) throws Exception {
        Class clazz = val.getClass();
        if (val == null) {
            writer.nullValue();
        }else if (Integer.class.isAssignableFrom(clazz)) {
            writer.value((int) val);
        } else if (Float.class.isAssignableFrom(clazz)) {
            writer.value((float) val);
        } else if (Double.class.isAssignableFrom(clazz)) {
            writer.value((double) val);
        } else if (String.class.isAssignableFrom(clazz)) {
            writer.value((String) val);
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            writer.value((Boolean) val);
        } else {
            throw new Exception("invalid type: " + field.getType().toString() + " for filed: "
                    + field.getName());
        }
    }

    /**
     * Get a value of specified type from JSONArray
     * @param type
     * @param json
     * @param key
     * @return
     * @throws Exception
     */
    private static Object getValue (Class type, JSONArray json, int key) throws Exception {
        Object val;
        if (String.class.isAssignableFrom(type)) {
            val = json.getString(key);
        }else if (Integer.class.isAssignableFrom(type)) {
            val = json.getInt(key);
        }else if (Float.class.isAssignableFrom(type)) {
            val = (float)json.getDouble(key);
        }else if (Double.class.isAssignableFrom(type)) {
            val = json.getDouble(key);
        }else if (Boolean.class.isAssignableFrom(type)) {
            val = json.getBoolean(key);
        }else{
            throw new Exception("Invalid fieldType: "+type.toString()+
                    " for index: "+key);
        }
        return val;
    }

    /**
     * Get a value of specified type from JSONObject
     * @param type
     * @param json
     * @param key
     * @return
     * @throws Exception
     */
    private static Object getValue (Class type, JSONObject json, String key) throws Exception {
        Object val;
        if (String.class.isAssignableFrom(type)) {
            val = json.getString(key);
        }else if (Integer.class.isAssignableFrom(type)) {
            val = json.getInt(key);
        }else if (Float.class.isAssignableFrom(type)) {
            val = (float)json.getDouble(key);
        }else if (Double.class.isAssignableFrom(type)) {
            val = json.getDouble(key);
        }else if (Boolean.class.isAssignableFrom(type)) {
            val = json.getBoolean(key);
        }else{
            throw new Exception("Invalid fieldType: "+type.toString()+
                    " for index: "+key);
        }
        return val;
    }
}