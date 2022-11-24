package dependency_injection;

import testclass.A;
import testclass.I;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

/**
 * TODO you should complete the class
 */
public class BeanFactoryImpl implements BeanFactory {

    Properties injectProperties;

    Properties valueProperties;

    @Override
    public void loadInjectProperties(File file) {
        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            Properties properties = new Properties();
            properties.load(inputStream);
            injectProperties = properties;
//            properties.list(System.out);
//            String property = properties.getProperty("testclass.E");
//            System.out.println(property);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void loadValueProperties(File file) {
        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            Properties properties = new Properties();
            properties.load(inputStream);
            valueProperties = properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T createInstance(Class<T> clazz) {
        try {
            Constructor<?> constructor = null;
            if (clazz.getDeclaredConstructors().length == 0 || Modifier.isAbstract(clazz.getModifiers())) {
                ArrayList<Class> allClass = getAllClassByPath(clazz.getPackage().getName());
                for (int i = 0; i < allClass.size(); i++) {
                    if(Modifier.isAbstract(allClass.get(i).getModifiers())){
                        continue;
                    }
                    if (clazz.isAssignableFrom(allClass.get(i))) {
                        if (!clazz.equals(allClass.get(i))) {
                            clazz = allClass.get(i);
                            constructor = allClass.get(i).getDeclaredConstructors()[0];
                            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                                if (c.getAnnotation(Inject.class) != null) {
                                    constructor = c;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            else {
                constructor = clazz.getDeclaredConstructors()[0];
                for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                    if (c.getAnnotation(Inject.class) != null) {
                        constructor = c;
                        break;
                    }
                }
            }


            Parameter[] parameters = constructor.getParameters();
            ArrayList<Object> parameterObjects = new ArrayList<>();
            for (Parameter p: parameters) {
                if (p.getAnnotation(Value.class) != null) {
                    Value valueAnnotation = p.getAnnotation(Value.class);
                    if (valueProperties.containsKey(valueAnnotation.value())) {
                        String actual = valueProperties.getProperty(valueAnnotation.value());
                        if (p.getType() == int.class) {
                            String[] value = actual.split(valueAnnotation.delimiter());
                            boolean flag = false;
                            for (String s: value) {
                                if (this.isInteger(s)) {
                                    parameterObjects.add(Integer.parseInt(s));
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                parameterObjects.add(0);
                            }
                        }
                        else if (p.getType() == boolean.class) {
                            String[] value = actual.split(valueAnnotation.delimiter());
                            boolean flag = false;
                            for (String s: value) {
                                if (s.toLowerCase().equals("true")) {
                                    parameterObjects.add(true);
                                    flag = true;
                                    break;
                                }
                                else if (s.toLowerCase().equals("false")) {
                                    parameterObjects.add(false);
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                parameterObjects.add(false);
                            }
                        }
                        else if (p.getType() == String.class) {
                            String[] value = actual.split(valueAnnotation.delimiter());
                            parameterObjects.add(value[0]);
                        }
                        else if (p.getType() == int[].class) {
                            String[] value = actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            ArrayList<Integer> intArray = new ArrayList<>();
                            for (String s : value) {
                                if (isInteger(s)) {
                                    intArray.add(Integer.parseInt(s));
                                }
                            }
                            int[] ints = new int[intArray.size()];
                            for (int i = 0; i < ints.length; i++) {
                                ints[i] = intArray.get(i);
                            }
                            parameterObjects.add(ints);
                        }
                        else if (p.getType() == boolean[].class) {
                            String[] value = actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            ArrayList<Boolean> booleanArray = new ArrayList<>();
                            for (String s: value) {
                                if (s.toLowerCase().equals("true")) {
                                    booleanArray.add(true);
                                }
                                else if (s.toLowerCase().equals("false")) {
                                    booleanArray.add(false);
                                }
                            }
                            boolean[] booleans = new boolean[booleanArray.size()];
                            for (int i = 0; i < booleans.length; i++) {
                                booleans[i] = booleanArray.get(i);
                            }
                            parameterObjects.add(booleans);
                        }
                        else if (p.getType() == String[].class) {
                            String[] value = actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            parameterObjects.add(value);
                        }
                        else if (p.getType() == List.class) {

                            Class<?> elementType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                    .getActualTypeArguments()[0].getTypeName());
                            String[] element =
                                    actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            List<Object> elementList = new ArrayList<>();
                            if (elementType == String.class) {
                                elementList = new ArrayList<>();
                                for (String s: element) {
                                    if (!s.equals("")) {
                                        elementList.add(s);
                                    }
                                }
                            }
                            else if (elementType == Integer.class) {
                                elementList = new ArrayList<>();
                                for (String s: element) {
                                    if (this.isInteger(s)) {
                                        elementList.add(Integer.parseInt(s));
                                    }
                                }
                            }
                            else if (elementType == Boolean.class) {
                                elementList = new ArrayList<>();
                                for (String s: element) {
                                    if (s.toLowerCase().equals("true")) {
                                        elementList.add(true);

                                    }
                                    else if (s.toLowerCase().equals("false")) {
                                        elementList.add(false);
                                    }
                                }
                            }
                            Object fieldObj = elementList;
                            parameterObjects.add(fieldObj);

                        }
                        else if (p.getType() == Set.class) {

                            Class<?> elementType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                    .getActualTypeArguments()[0].getTypeName());
                            String[] element =
                                    actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            Set<Object> elementList = new HashSet<>();
                            if (elementType == String.class) {
                                elementList = new HashSet<>();
                                for (String s: element) {
                                    if (!s.equals("")) {
                                        elementList.add(s);
                                    }
                                }
                            }
                            else if (elementType == Integer.class) {
                                elementList = new HashSet<>();
                                for (String s: element) {
                                    if (this.isInteger(s)) {
                                        elementList.add(Integer.parseInt(s));
                                    }
                                }
                            }
                            else if (elementType == Boolean.class) {
                                elementList = new HashSet<>();
                                for (String s: element) {
                                    if (s.toLowerCase().equals("true")) {
                                        elementList.add(true);

                                    }
                                    else if (s.toLowerCase().equals("false")) {
                                        elementList.add(false);
                                    }
                                }
                            }
                            Object fieldObj = elementList;
                            parameterObjects.add(fieldObj);
                        }
                        else if (p.getType() == Map.class) {

                            Class<?> keyType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                    .getActualTypeArguments()[0].getTypeName());
                            Class<?> valueType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                    .getActualTypeArguments()[1].getTypeName());
                            String[] element =
                                    actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            Map<Object, Object> mapObj = new HashMap<>();
                            for (String s : element) {
                                String key = s.split(":")[0];
                                String value = s.split(":")[1];

                                if (keyType == String.class) {
                                    if (valueType == String.class) {
                                        mapObj.put(key, value);
                                    }
                                    else if (valueType == Integer.class) {
                                        if (isInteger(value)){
                                            mapObj.put(key, Integer.parseInt(value));
                                        }
                                    }
                                    else if (valueType == Boolean.class) {
                                        if (value.toLowerCase().equals("true")) {
                                            mapObj.put(key, true);
                                        }
                                        else if (value.toLowerCase().equals("false")) {
                                            mapObj.put(key, false);
                                        }
                                    }
                                }
                                else if (keyType == Integer.class && isInteger(key)) {
                                    Integer keyValue = Integer.parseInt(key);
                                    if (valueType == String.class) {
                                        mapObj.put(keyValue, value);
                                    }
                                    else if (valueType == Integer.class) {
                                        if (isInteger(value)){
                                            mapObj.put(keyValue, Integer.parseInt(value));
                                        }
                                    }
                                    else if (valueType == Boolean.class) {
                                        if (value.toLowerCase().equals("true")) {
                                            mapObj.put(keyValue, true);
                                        }
                                        else if (value.toLowerCase().equals("false")) {
                                            mapObj.put(keyValue, false);
                                        }
                                    }
                                }
                                else if (keyType == Boolean.class && isBoolean(key)) {
                                    Boolean keyValue = Boolean.parseBoolean(key);
                                    if (valueType == String.class) {
                                        mapObj.put(keyValue, value);
                                    }
                                    else if (valueType == Integer.class) {
                                        if (isInteger(value)){
                                            mapObj.put(keyValue, Integer.parseInt(value));
                                        }
                                    }
                                    else if (valueType == Boolean.class) {
                                        if (value.toLowerCase().equals("true")) {
                                            mapObj.put(keyValue, true);
                                        }
                                        else if (value.toLowerCase().equals("false")) {
                                            mapObj.put(keyValue, false);
                                        }
                                    }
                                }
                            }
                            parameterObjects.add(mapObj);
                        }
                    }
                    else if (p.getType() == int.class) {
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        boolean flag = false;
                        for (String s: value) {
                            if (this.isInteger(s)) {
                                parameterObjects.add(Integer.parseInt(s));
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            parameterObjects.add(0);
                        }
                    }
                    else if (p.getType() == boolean.class) {
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        boolean flag = false;
                        for (String s: value) {
                            if (s.toLowerCase().equals("true")) {
                                parameterObjects.add(true);
                                flag = true;
                                break;
                            }
                            else if (s.toLowerCase().equals("false")) {
                                parameterObjects.add(false);
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            parameterObjects.add(false);
                        }
                    }
                    else if (p.getType() == String.class) {
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        parameterObjects.add(value[0]);
                    }
                    else if (p.getType() == int[].class) {
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        ArrayList<Integer> intArray = new ArrayList<>();
                        for (String s : value) {
                            if (isInteger(s)) {
                                intArray.add(Integer.parseInt(s));
                            }
                        }
                        int[] ints = new int[intArray.size()];
                        for (int i = 0; i < ints.length; i++) {
                            ints[i] = intArray.get(i);
                        }
                        parameterObjects.add(ints);
                    }
                    else if (p.getType() == boolean[].class) {
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        ArrayList<Boolean> booleanArray = new ArrayList<>();
                        for (String s: value) {
                            if (s.toLowerCase().equals("true")) {
                                booleanArray.add(true);
                            }
                            else if (s.toLowerCase().equals("false")) {
                                booleanArray.add(false);
                            }
                        }
                        boolean[] booleans = new boolean[booleanArray.size()];
                        for (int i = 0; i < booleans.length; i++) {
                            booleans[i] = booleanArray.get(i);
                        }
                        parameterObjects.add(booleans);
                    }
                    else if (p.getType() == String[].class) {
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        parameterObjects.add(value);
                    }
                    else if (p.getType() == List.class) {

                        Class<?> elementType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                .getActualTypeArguments()[0].getTypeName());
                        String[] element =
                                valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        List<Object> elementList = new ArrayList<>();
                        if (elementType == String.class) {
                            elementList = new ArrayList<>();
                            for (String s: element) {
                                if (!s.equals("")) {
                                    elementList.add(s);
                                }
                            }
                        }
                        else if (elementType == Integer.class) {
                            elementList = new ArrayList<>();
                            for (String s: element) {
                                if (this.isInteger(s)) {
                                    elementList.add(Integer.parseInt(s));
                                }
                            }
                        }
                        else if (elementType == Boolean.class) {
                            elementList = new ArrayList<>();
                            for (String s: element) {
                                if (s.toLowerCase().equals("true")) {
                                    elementList.add(true);

                                }
                                else if (s.toLowerCase().equals("false")) {
                                    elementList.add(false);
                                }
                            }
                        }
                        Object fieldObj = elementList;
                        parameterObjects.add(fieldObj);

                    }
                    else if (p.getType() == Set.class) {

                        Class<?> elementType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                .getActualTypeArguments()[0].getTypeName());
                        String[] element =
                                valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        Set<Object> elementList = new HashSet<>();
                        if (elementType == String.class) {
                            elementList = new HashSet<>();
                            for (String s: element) {
                                if (!s.equals("")) {
                                    elementList.add(s);
                                }
                            }
                        }
                        else if (elementType == Integer.class) {
                            elementList = new HashSet<>();
                            for (String s: element) {
                                if (this.isInteger(s)) {
                                    elementList.add(Integer.parseInt(s));
                                }
                            }
                        }
                        else if (elementType == Boolean.class) {
                            elementList = new HashSet<>();
                            for (String s: element) {
                                if (s.toLowerCase().equals("true")) {
                                    elementList.add(true);

                                }
                                else if (s.toLowerCase().equals("false")) {
                                    elementList.add(false);
                                }
                            }
                        }
                        Object fieldObj = elementList;
                        parameterObjects.add(fieldObj);
                    }
                    else if (p.getType() == Map.class) {

                        Class<?> keyType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                .getActualTypeArguments()[0].getTypeName());
                        Class<?> valueType = Class.forName(((ParameterizedType) p.getParameterizedType())
                                .getActualTypeArguments()[1].getTypeName());
                        String[] element =
                                valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        Map<Object, Object> mapObj = new HashMap<>();
                        for (String s : element) {
                            String key = s.split(":")[0];
                            String value = s.split(":")[1];

                            if (keyType == String.class) {
                                if (valueType == String.class) {
                                    mapObj.put(key, value);
                                }
                                else if (valueType == Integer.class) {
                                    if (isInteger(value)){
                                        mapObj.put(key, Integer.parseInt(value));
                                    }
                                }
                                else if (valueType == Boolean.class) {
                                    if (value.toLowerCase().equals("true")) {
                                        mapObj.put(key, true);
                                    }
                                    else if (value.toLowerCase().equals("false")) {
                                        mapObj.put(key, false);
                                    }
                                }
                            }
                            else if (keyType == Integer.class && isInteger(key)) {
                                Integer keyValue = Integer.parseInt(key);
                                if (valueType == String.class) {
                                    mapObj.put(keyValue, value);
                                }
                                else if (valueType == Integer.class) {
                                    if (isInteger(value)){
                                        mapObj.put(keyValue, Integer.parseInt(value));
                                    }
                                }
                                else if (valueType == Boolean.class) {
                                    if (value.toLowerCase().equals("true")) {
                                        mapObj.put(keyValue, true);
                                    }
                                    else if (value.toLowerCase().equals("false")) {
                                        mapObj.put(keyValue, false);
                                    }
                                }
                            }
                            else if (keyType == Boolean.class && isBoolean(key)) {
                                Boolean keyValue = Boolean.parseBoolean(key);
                                if (valueType == String.class) {
                                    mapObj.put(keyValue, value);
                                }
                                else if (valueType == Integer.class) {
                                    if (isInteger(value)){
                                        mapObj.put(keyValue, Integer.parseInt(value));
                                    }
                                }
                                else if (valueType == Boolean.class) {
                                    if (value.toLowerCase().equals("true")) {
                                        mapObj.put(keyValue, true);
                                    }
                                    else if (value.toLowerCase().equals("false")) {
                                        mapObj.put(keyValue, false);
                                    }
                                }
                            }
                        }
                        parameterObjects.add(mapObj);
                    }
                }
                else {
                    parameterObjects.add(createInstance(p.getType()));
                }
            }
            Object[] parameterObjects2 = parameterObjects.toArray();
            T instance = (T) constructor.newInstance(parameterObjects2);
            Field[] fields = clazz.getDeclaredFields();
            for (Field f: fields) {
                if (f.getDeclaredAnnotation(Inject.class) != null) {
                    f.setAccessible(true);
                    f.set(instance, createInstance(f.getType()));
                    f.setAccessible(false);
                }
                if (f.getDeclaredAnnotation(Value.class) != null) {
                    Value valueAnnotation = f.getAnnotation(Value.class);
                    if (valueProperties.containsKey(valueAnnotation.value())) {
                        f.setAccessible(true);
                        String actual = valueProperties.getProperty(valueAnnotation.value());
                        if (f.getType() == int.class) {
                            String[] value = actual.split(valueAnnotation.delimiter());
                            boolean flag = false;
                            for (String s: value) {
                                if (this.isInteger(s)) {
                                    f.set(instance, Integer.parseInt(s));
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                f.set(instance, 0);
                            }
                        }
                        else if (f.getType() == boolean.class) {
                            String[] value = actual.split(valueAnnotation.delimiter());
                            boolean flag = false;
                            for (String s: value) {
                                if (s.toLowerCase().equals("true")) {
                                    f.set(instance, true);
                                    flag = true;
                                    break;
                                }
                                else if (s.toLowerCase().equals("false")) {
                                    f.set(instance, false);
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                f.set(instance, false);
                            }
                        }
                        else if (f.getType() == String.class) {
                            String[] value = actual.split(valueAnnotation.delimiter());
                            f.set(instance, value);
                        }
                        else if (f.getType() == int[].class) {
                            String[] value = actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            ArrayList<Integer> intArray = new ArrayList<>();
                            for (String s : value) {
                                if (isInteger(s)) {
                                    intArray.add(Integer.parseInt(s));
                                }
                            }
                            int[] ints = new int[intArray.size()];
                            for (int i = 0; i < ints.length; i++) {
                                ints[i] = intArray.get(i);
                            }
                            f.set(instance, ints);
                        }
                        else if (f.getType() == boolean[].class) {
                            String[] value = actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            ArrayList<Boolean> booleanArray = new ArrayList<>();
                            for (String s: value) {
                                if (s.toLowerCase().equals("true")) {
                                    booleanArray.add(true);
                                }
                                else if (s.toLowerCase().equals("false")) {
                                    booleanArray.add(false);
                                }
                            }
                            boolean[] booleans = new boolean[booleanArray.size()];
                            for (int i = 0; i < booleans.length; i++) {
                                booleans[i] = booleanArray.get(i);
                            }
                            f.set(instance, booleans);
                        }
                        else if (f.getType() == String[].class) {
                            String[] value = actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            f.set(instance, value);
                        }
                        else if (f.getType() == List.class) {

                            Class<?> elementType = Class.forName(((ParameterizedType) f.getGenericType())
                                    .getActualTypeArguments()[0].getTypeName());
                            String[] element =
                                    actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            List<Object> elementList = new ArrayList<>();
                            if (elementType == String.class) {
                                elementList = new ArrayList<>();
                                for (String s: element) {
                                    if (!s.equals("")) {
                                        elementList.add(s);
                                    }
                                }
                            }
                            else if (elementType == Integer.class) {
                                elementList = new ArrayList<>();
                                for (String s: element) {
                                    if (this.isInteger(s)) {
                                        elementList.add(Integer.parseInt(s));
                                    }
                                }
                            }
                            else if (elementType == Boolean.class) {
                                elementList = new ArrayList<>();
                                for (String s: element) {
                                    if (s.toLowerCase().equals("true")) {
                                        elementList.add(true);

                                    }
                                    else if (s.toLowerCase().equals("false")) {
                                        elementList.add(false);
                                    }
                                }
                            }
                            Object fieldObj = elementList;
                            f.set(instance, fieldObj);

                        }
                        else if (f.getType() == Set.class) {

                            Class<?> elementType = Class.forName(((ParameterizedType) f.getGenericType())
                                    .getActualTypeArguments()[0].getTypeName());
                            String[] element =
                                    actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            Set<Object> elementList = new HashSet<>();
                            if (elementType == String.class) {
                                elementList = new HashSet<>();
                                for (String s: element) {
                                    if (!s.equals("")) {
                                        elementList.add(s);
                                    }
                                }
                            }
                            else if (elementType == Integer.class) {
                                elementList = new HashSet<>();
                                for (String s: element) {
                                    if (this.isInteger(s)) {
                                        elementList.add(Integer.parseInt(s));
                                    }
                                }
                            }
                            else if (elementType == Boolean.class) {
                                elementList = new HashSet<>();
                                for (String s: element) {
                                    if (s.toLowerCase().equals("true")) {
                                        elementList.add(true);

                                    }
                                    else if (s.toLowerCase().equals("false")) {
                                        elementList.add(false);
                                    }
                                }
                            }
                            Object fieldObj = elementList;
                            f.set(instance, fieldObj);
                        }
                        else if (f.getType() == Map.class) {

                            Class<?> keyType = Class.forName(((ParameterizedType) f.getGenericType())
                                    .getActualTypeArguments()[0].getTypeName());
                            Class<?> valueType = Class.forName(((ParameterizedType) f.getGenericType())
                                    .getActualTypeArguments()[1].getTypeName());
                            String[] element =
                                    actual.substring(1, actual.length() - 1).split(valueAnnotation.delimiter());
                            Map<Object, Object> mapObj = new HashMap<>();
                            for (String s : element) {
                                String key = s.split(":")[0];
                                String value = s.split(":")[1];

                                if (keyType == String.class) {
                                    if (valueType == String.class) {
                                        mapObj.put(key, value);
                                    }
                                    else if (valueType == Integer.class) {
                                        if (isInteger(value)){
                                            mapObj.put(key, Integer.parseInt(value));
                                        }
                                    }
                                    else if (valueType == Boolean.class) {
                                        if (value.toLowerCase().equals("true")) {
                                            mapObj.put(key, true);
                                        }
                                        else if (value.toLowerCase().equals("false")) {
                                            mapObj.put(key, false);
                                        }
                                    }
                                }
                                else if (keyType == Integer.class && isInteger(key)) {
                                    Integer keyValue = Integer.parseInt(key);
                                    if (valueType == String.class) {
                                        mapObj.put(keyValue, value);
                                    }
                                    else if (valueType == Integer.class) {
                                        if (isInteger(value)){
                                            mapObj.put(keyValue, Integer.parseInt(value));
                                        }
                                    }
                                    else if (valueType == Boolean.class) {
                                        if (value.toLowerCase().equals("true")) {
                                            mapObj.put(keyValue, true);
                                        }
                                        else if (value.toLowerCase().equals("false")) {
                                            mapObj.put(keyValue, false);
                                        }
                                    }
                                }
                                else if (keyType == Boolean.class && isBoolean(key)) {
                                    Boolean keyValue = Boolean.parseBoolean(key);
                                    if (valueType == String.class) {
                                        mapObj.put(keyValue, value);
                                    }
                                    else if (valueType == Integer.class) {
                                        if (isInteger(value)){
                                            mapObj.put(keyValue, Integer.parseInt(value));
                                        }
                                    }
                                    else if (valueType == Boolean.class) {
                                        if (value.toLowerCase().equals("true")) {
                                            mapObj.put(keyValue, true);
                                        }
                                        else if (value.toLowerCase().equals("false")) {
                                            mapObj.put(keyValue, false);
                                        }
                                    }
                                }
                            }
                            f.set(instance, mapObj);
                        }
                        f.setAccessible(false);
                    }
                    else if (f.getType() == int.class) {
                        f.setAccessible(true);
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        boolean flag = false;
                        for (String s: value) {
                            if (this.isInteger(s)) {
                                f.set(instance, Integer.parseInt(s));
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            f.set(instance, 0);
                        }
                        f.setAccessible(false);
                    }
                    else if (f.getType() == boolean.class) {
                        f.setAccessible(true);
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        boolean flag = false;
                        for (String s: value) {
                            if (s.toLowerCase().equals("true")) {
                                f.set(instance, true);
                                flag = true;
                                break;
                            }
                            else if (s.toLowerCase().equals("false")) {
                                f.set(instance, false);
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            f.set(instance, false);
                        }
                        f.setAccessible(false);
                    }
                    else if (f.getType() == String.class) {
                        f.setAccessible(true);
                        String[] value = valueAnnotation.value().split(valueAnnotation.delimiter());
                        f.set(instance, value[0]);
                        f.setAccessible(false);
                    }
                    else if (f.getType() == int[].class) {
                        f.setAccessible(true);
                        String[] value = valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        ArrayList<Integer> intArray = new ArrayList<>();
                        for (String s : value) {
                            if (isInteger(s)) {
                                intArray.add(Integer.parseInt(s));
                            }
                        }
                        int[] ints = new int[intArray.size()];
                        for (int i = 0; i < ints.length; i++) {
                            ints[i] = intArray.get(i);
                        }
                        f.set(instance, ints);
                        f.setAccessible(false);
                    }
                    else if (f.getType() == boolean[].class) {
                        f.setAccessible(true);
                        String[] value = valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        ArrayList<Boolean> booleanArray = new ArrayList<>();
                        for (String s: value) {
                            if (s.toLowerCase().equals("true")) {
                                booleanArray.add(true);
                            }
                            else if (s.toLowerCase().equals("false")) {
                                booleanArray.add(false);
                            }
                        }
                        boolean[] booleans = new boolean[booleanArray.size()];
                        for (int i = 0; i < booleans.length; i++) {
                            booleans[i] = booleanArray.get(i);
                        }
                        f.set(instance, booleans);
                        f.setAccessible(false);
                    }
                    else if (f.getType() == String[].class) {
                        f.setAccessible(true);
                        String[] value = valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        f.set(instance, value);
                        f.setAccessible(false);
                    }
                    else if (f.getType() == List.class) {
                        Class<?> elementType = Class.forName(((ParameterizedType) f.getGenericType())
                                .getActualTypeArguments()[0].getTypeName());
                        String[] element =
                                valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        List<Object> elementList = new ArrayList<>();
                        if (elementType == String.class) {
                            elementList = new ArrayList<>();
                            for (String s: element) {
                                if (!s.equals("")) {
                                    elementList.add(s);
                                }
                            }
                        }
                        else if (elementType == Integer.class) {
                            elementList = new ArrayList<>();
                            for (String s: element) {
                                if (this.isInteger(s)) {
                                    elementList.add(Integer.parseInt(s));
                                }
                            }
                        }
                        else if (elementType == Boolean.class) {
                            elementList = new ArrayList<>();
                            for (String s: element) {
                                if (s.toLowerCase().equals("true")) {
                                    elementList.add(true);

                                }
                                else if (s.toLowerCase().equals("false")) {
                                    elementList.add(false);
                                }
                            }
                        }
                        Object fieldObj = elementList;
                        f.setAccessible(true);
                        f.set(instance, fieldObj);
                        f.setAccessible(false);
                    }
                    else if (f.getType() == Set.class) {

                        Class<?> elementType = Class.forName(((ParameterizedType) f.getGenericType())
                                .getActualTypeArguments()[0].getTypeName());
                        String[] element =
                                valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        Set<Object> elementList = new HashSet<>();
                        if (elementType == String.class) {
                            elementList = new HashSet<>();
                            for (String s: element) {
                                if (!s.equals("")) {
                                    elementList.add(s);
                                }
                            }
                        }
                        else if (elementType == Integer.class) {
                            elementList = new HashSet<>();
                            for (String s: element) {
                                if (this.isInteger(s)) {
                                    elementList.add(Integer.parseInt(s));
                                }
                            }
                        }
                        else if (elementType == Boolean.class) {
                            elementList = new HashSet<>();
                            for (String s: element) {
                                if (s.toLowerCase().equals("true")) {
                                    elementList.add(true);

                                }
                                else if (s.toLowerCase().equals("false")) {
                                    elementList.add(false);
                                }
                            }
                        }
                        Object fieldObj = elementList;
                        f.setAccessible(true);
                        f.set(instance, fieldObj);
                        f.setAccessible(false);
                    }
                    else if (f.getType() == Map.class) {

                        Class<?> keyType = Class.forName(((ParameterizedType) f.getGenericType())
                                .getActualTypeArguments()[0].getTypeName());
                        Class<?> valueType = Class.forName(((ParameterizedType) f.getGenericType())
                                .getActualTypeArguments()[1].getTypeName());
                        String[] element =
                                valueAnnotation.value().substring(1, valueAnnotation.value().length() - 1).split(valueAnnotation.delimiter());
                        Map<Object, Object> mapObj = new HashMap<>();
                        for (String s : element) {
                            String key = s.split(":")[0];
                            String value = s.split(":")[1];

                            if (keyType == String.class) {
                                if (valueType == String.class) {
                                    mapObj.put(key, value);
                                }
                                else if (valueType == Integer.class) {
                                    if (isInteger(value)){
                                        mapObj.put(key, Integer.parseInt(value));
                                    }
                                }
                                else if (valueType == Boolean.class) {
                                    if (value.toLowerCase().equals("true")) {
                                        mapObj.put(key, true);
                                    }
                                    else if (value.toLowerCase().equals("false")) {
                                        mapObj.put(key, false);
                                    }
                                }
                            }
                            else if (keyType == Integer.class && isInteger(key)) {
                                Integer keyValue = Integer.parseInt(key);
                                if (valueType == String.class) {
                                    mapObj.put(keyValue, value);
                                }
                                else if (valueType == Integer.class) {
                                    if (isInteger(value)){
                                        mapObj.put(keyValue, Integer.parseInt(value));
                                    }
                                }
                                else if (valueType == Boolean.class) {
                                    if (value.toLowerCase().equals("true")) {
                                        mapObj.put(keyValue, true);
                                    }
                                    else if (value.toLowerCase().equals("false")) {
                                        mapObj.put(keyValue, false);
                                    }
                                }
                            }
                            else if (keyType == Boolean.class && isBoolean(key)) {
                                Boolean keyValue = Boolean.parseBoolean(key);
                                if (valueType == String.class) {
                                    mapObj.put(keyValue, value);
                                }
                                else if (valueType == Integer.class) {
                                    if (isInteger(value)){
                                        mapObj.put(keyValue, Integer.parseInt(value));
                                    }
                                }
                                else if (valueType == Boolean.class) {
                                    if (value.toLowerCase().equals("true")) {
                                        mapObj.put(keyValue, true);
                                    }
                                    else if (value.toLowerCase().equals("false")) {
                                        mapObj.put(keyValue, false);
                                    }
                                }
                            }
                        }
                        f.setAccessible(true);
                        f.set(instance, mapObj);
                        f.setAccessible(false);
                    }
                }
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean isBoolean(String s) {
        if (s.toLowerCase().equals("true") || s.toLowerCase().equals("false") ){
            return true;
        }
        else {
            return false;
        }
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static ArrayList<Class> getAllClassByPath(String packagename){
        ArrayList<Class> list = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packagename.replace('.', '/');
        try {
            ArrayList<File> fileList = new ArrayList<>();
            Enumeration<URL> enumeration = classLoader.getResources(path);
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                fileList.add(new File(url.getFile()));
            }
            for (int i = 0; i < fileList.size(); i++) {
                list.addAll(findClass(fileList.get(i),packagename));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
    private static ArrayList<Class> findClass(File file,String packagename) {
        ArrayList<Class> list = new ArrayList<>();
        if (!file.exists()) {
            return list;
        }
        File[] files = file.listFiles();
        for (File file2 : files) {
            if (file2.isDirectory()) {
                ArrayList<Class> arrayList = findClass(file2, packagename+"."+file2.getName());
                list.addAll(arrayList);
            }else if(file2.getName().endsWith(".class")){
                try {
                    list.add(Class.forName(packagename + '.' + file2.getName().substring(0,
                            file2.getName().length()-6)));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }
}
