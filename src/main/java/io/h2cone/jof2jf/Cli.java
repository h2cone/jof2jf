package io.h2cone.jof2jf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.cli.*;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cli {
    private static class ObjectMapperHolder {
        static final ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ObjectMapper mapper() {
        return ObjectMapperHolder.mapper;
    }

    public static void main(String... args) {
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption("c", true, "class name");
        options.addOption("p", true, "package name");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.printf("Failed to parse: %s, exception: %s\n", Arrays.toString(args), e);
            return;
        }

        List<String> argList = cmd.getArgList();
        if (argList.size() < 2) {
            System.err.println("JSON object file pathname and Java file directory not provided");
            printHelp(options);
            return;
        }
        String jsonObjectFilePath = argList.get(0);
        String javaFileDirectory = argList.get(1);

        File jsonObjectFile = new File(jsonObjectFilePath);
        try {
            FileInputStream in = new FileInputStream(jsonObjectFile);
            Map<String, Object> body = mapper().readValue(in, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            String className = cmd.getOptionValue("c", "Example");
            if (isBlank(className)) {
                System.err.println("Class name cannot be blank");
                return;
            }
            TypeSpec typeSpec = typeSpec(body, className);

            String packageName = cmd.getOptionValue("p", "com.example");
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                    .skipJavaLangImports(true)
                    .build();
            javaFile.writeTo(new File(javaFileDirectory));
            System.out.println("Successfully generated");

        } catch (FileNotFoundException e) {
            System.err.printf("File not found: %s\n", jsonObjectFilePath);
        } catch (IOException e) {
            System.err.printf("Failed to generate Java file: %s, exception: %s\n", javaFileDirectory, e);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jof2jf.jar <jsonObjectFilePathname> <javaFileDirectory> [options]", options);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    static TypeSpec typeSpec(Map<String, Object> body, String className) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);

        body.forEach((k, v) -> build(k, v, builder));

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static void build(String key, Object value, TypeSpec.Builder builder) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }

        ClassName wrapperClassName = wrapperClassName(value);
        if (wrapperClassName != null) {
            builder.addField(wrapperClassName, key);

        } else if (value instanceof List) {
            ParameterizedTypeName listType = buildList(key, (List) value, builder);
            builder.addField(listType, key);

        } else {
            if (value == null) {
                builder.addField(ClassName.get(Object.class), key);
                return;
            }
            String suffix = key.substring(1);
            String prefix = key.substring(0, 1);
            String innerclassName = prefix.toUpperCase() + suffix;
            TypeSpec.Builder innerBuilder = TypeSpec.classBuilder(innerclassName).addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            ClassName className = ClassName.bestGuess(innerclassName);
            builder.addField(className, key);

            Map<String, Object> body = mapper().convertValue(value, LinkedHashMap.class);
            body.forEach((k, v) -> build(k, v, innerBuilder));
            builder.addType(innerBuilder.build());
        }
    }

    @SuppressWarnings("unchecked")
    private static ParameterizedTypeName buildList(String listName, List list, TypeSpec.Builder builder) {
        if (list.isEmpty()) {
            return ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Object.class));
        }
        Object elem = list.get(0);
        ClassName wrapperClassName = wrapperClassName(elem);
        if (wrapperClassName != null) {
            return ParameterizedTypeName.get(ClassName.get(List.class), wrapperClassName);

        } else if (elem instanceof List) {
            return ParameterizedTypeName.get(ClassName.get(List.class), buildList(listName, (List) elem, builder));

        } else {
            if (elem == null) {
                return ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Object.class));
            }
            String prefix = listName.substring(0, 1);
            String suffix = listName.substring(1);
            String elementClassName = prefix.toUpperCase() + suffix + "Elem";
            TypeSpec.Builder innerBuilder = TypeSpec.classBuilder(elementClassName).addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            Map<String, Object> body = mapper().convertValue(elem, LinkedHashMap.class);
            body.forEach((k, v) -> build(k, v, innerBuilder));
            builder.addType(innerBuilder.build());

            return ParameterizedTypeName.get(ClassName.get(List.class), ClassName.bestGuess(elementClassName));
        }
    }

    private static ClassName wrapperClassName(Object o) {
        if (o instanceof Integer) {
            return ClassName.get(Integer.class);

        } else if (o instanceof Long) {
            return ClassName.get(Long.class);

        } else if (o instanceof Boolean) {
            return ClassName.get(Boolean.class);

        } else if (o instanceof Double) {
            return ClassName.get(Double.class);

        } else if (o instanceof String) {
            return ClassName.get(String.class);

        } else {
            return null;
        }
    }
}
