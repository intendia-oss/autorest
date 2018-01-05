package com.intendia.gwt.autorest.processor;

import static java.util.Objects.isNull;

import com.google.common.base.Throwables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public class Register {

    private Set<String> mappers;
    private final Messager messager;
    private final ProcessingEnvironment processingEnv;
    private final String fileName;

    public Register(String fileName, Set<String> mappers, Messager messager,
                    ProcessingEnvironment processingEnv) {
        this.mappers = mappers;

        this.messager = messager;
        this.processingEnv = processingEnv;
        this.fileName = fileName;
    }

    public Set<String> readItems() {
        if (isNull(mappers)) {
            mappers = new TreeSet<>();
            try {
                FileObject resource = processingEnv.getFiler()
                        .getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/jackson-mapper/" + fileName);
                new BufferedReader(new InputStreamReader(resource.openInputStream())).lines().forEach(mappers::add);
            } catch (IOException notFoundException) {
                messager.printMessage(Diagnostic.Kind.NOTE, "File not found 'META-INF/jackson-mapper/" + fileName + "'");
            }
        }
        return mappers;
    }


    public void writeItems() {
        try {
            FileObject resource = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/jackson-mapper/" + fileName);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(resource.openOutputStream()));
            mappers.forEach(out::println);
            out.close();
        } catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, Throwables.getStackTraceAsString(ex));
        }
    }

}
