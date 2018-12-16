package ru.runa.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.upload.MultipartRequestHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonConverter {

  public static <T extends ActionForm> List<T> getFormList(T actionForm,
      MultipartRequestHandler multipartRequestHandler, String jsonString) {
    List<T> formList = new ArrayList<>();
    JSONArray jsonArray = new JSONArray(jsonString);
    ObjectMapper objectMapper = new ObjectMapper();
    JSONObject jsonObject = null;
    Map<String, String> map = Collections.EMPTY_MAP;
    Class<?> clazz = null;
    Constructor<?> ctor = null;
    T form = null;

    try {
      clazz = Class.forName("ru.runa.wf.web.form." + actionForm.getClass().getSimpleName());
      ctor = clazz.getConstructor();

    } catch (ClassNotFoundException | NoSuchMethodException e) {
      e.printStackTrace();
    }

    for (Object elem : jsonArray) {

      try {
        form = (T) ctor.newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }
      map = jsonToMap((JSONObject) elem);
      Map<String, String> processFormMap = map.entrySet().stream().filter(
          k -> k.getKey().equals("id") || k.getKey().equals("actorId") || k.getKey()
              .equals("submitButton") || k.getKey().equals("multipleSubmit"))
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      jsonObject = new JSONObject(processFormMap);
      try {
        form = (T) objectMapper.readValue(jsonObject.toString(), form.getClass());
        form.setMultipartRequestHandler(multipartRequestHandler);
        form.setServlet(form.getMultipartRequestHandler().getServlet());
        form.getMultipartRequestHandler().getAllElements().putAll(map);
        formList.add(form);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
    return formList;
  }

  private static Map<String, String> jsonToMap(JSONObject json) throws JSONException {
    Map<String, String> retMap = new HashMap<String, String>();

    if (json != JSONObject.NULL) {
      retMap = toMap(json);
    }
    return retMap;
  }

  private static Map<String, String> toMap(JSONObject object) throws JSONException {
    Map<String, String> map = new HashMap<String, String>();

    Iterator<String> keysItr = object.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = object.get(key);

      if (value instanceof JSONArray) {
        value = toList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = toMap((JSONObject) value);
      }
      map.put(key, value.toString());
    }
    return map;
  }

  private static List<Object> toList(JSONArray array) throws JSONException {
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < array.length(); i++) {
      Object value = array.get(i);
      if (value instanceof JSONArray) {
        value = toList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = toMap((JSONObject) value);
      }
      list.add(value);
    }
    return list;
  }
}
