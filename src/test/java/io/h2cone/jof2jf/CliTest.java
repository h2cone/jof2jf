/*
 * Copyright 2019 h2cone https://github.com/h2cone
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.h2cone.jof2jf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.TypeSpec;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CliTest {

    @Test
    public void main() {
        Cli.main("src/test/resources/Foobar.json", "src/test/java", "-c", "Foobar", "-p", "io.h2cone.jof2jf");
    }

    @Test
    public void compare() throws IOException {
        String className = Foobar.class.getSimpleName();
        InputStream jsonObjectIn = this.getClass().getClassLoader().getResourceAsStream("Foobar.json");

        ObjectMapper mapper = Cli.mapper();
        Map<String, Object> body = mapper.readValue(jsonObjectIn, new TypeReference<LinkedHashMap<String, Object>>() {
        });
        Assert.assertNotNull(body);

        TypeSpec typeSpec = Cli.typeSpec(body, className);
        Map<String, String> fieldNameFiledTypeNameMap = typeSpec.fieldSpecs.stream()
                .collect(Collectors.toMap(fs -> fs.name, fs -> fs.type.toString()));
        System.out.println(fieldNameFiledTypeNameMap);

        Map<String, String> declaredClassSimpleNameDeclaredClassNameMap = new HashMap<>();
        Class<?>[] declaredClasses = Foobar.class.getDeclaredClasses();
        for (Class aClass : declaredClasses) {
            String simpleName = aClass.getSimpleName();
            String name = aClass.getName();
            declaredClassSimpleNameDeclaredClassNameMap.put(simpleName, name);
        }
        System.out.println(declaredClassSimpleNameDeclaredClassNameMap);

        Field[] fields = Foobar.class.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            String actualFieldTypeName = field.getGenericType().getTypeName();
            String expectedFieldTypeName = fieldNameFiledTypeNameMap.get(fieldName);

            System.out.println(fieldName + " | " + actualFieldTypeName + " | " + expectedFieldTypeName);
            Assert.assertNotNull(expectedFieldTypeName);

            if (!expectedFieldTypeName.equals(actualFieldTypeName)) {
                for (Map.Entry<String, String> entry : declaredClassSimpleNameDeclaredClassNameMap.entrySet()) {
                    String simpleName = entry.getKey();
                    String name = entry.getValue();

                    expectedFieldTypeName = expectedFieldTypeName.replace(simpleName, name);
                }
                Assert.assertEquals(expectedFieldTypeName, actualFieldTypeName);
            }
        }
    }

    @Test
    public void argsRequired() {
        Cli.main();
    }

    @Test
    public void help() {
        Cli.main("--help");
    }
}
