package com.intendia.gwt.autorest.processor;

import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubject;
import org.junit.Test;

public class AutoRestGwtProcessorTest {
    @Test public void works() {
        JavaSourcesSubject
                .assertThat(JavaFileObjects.forSourceString("Rest", ""
                        + "import com.intendia.gwt.autorest.client.AutoRestGwt;"
                        + "import java.util.Optional;\n"
                        + "import javax.ws.rs.GET;\n"
                        + "import javax.ws.rs.Path;\n"
                        + "\n"
                        + "@AutoRestGwt\n"
                        + "@Path(\"a\") interface Rest {\n"
                        + "    @GET Optional<String> getStr();\n"
                        + "}"))
                .withCompilerOptions("-AskipJavaLangImports")
                .processedWith(new AutoRestGwtProcessor())
                .compilesWithoutError().and()
                .generatesSources(JavaFileObjects.forSourceString("Rest_RestServiceModel", ""
                        + "import static javax.ws.rs.HttpMethod.GET;\n"
                        + "\n"
                        + "import com.intendia.gwt.autorest.client.ResourceVisitor;\n"
                        + "import com.intendia.gwt.autorest.client.RestServiceModel;\n"
                        + "import com.intendia.gwt.autorest.client.TypeToken;\n"
                        + "import java.util.Optional;\n"
                        + "import javax.inject.Inject;\n"
                        + "\n"
                        + "public class Rest_RestServiceModel extends RestServiceModel implements Rest {\n"
                        + "\n"
                        + "    @Inject\n"
                        + "    public Rest_RestServiceModel(final ResourceVisitor.Supplier parent) {\n"
                        + "        super(new ResourceVisitor.Supplier() {\n"
                        + "            public ResourceVisitor get() { return parent.get().path(\"a\"); }\n"
                        + "        });\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override"
                        + "    public Optional<String> getStr() {\n"
                        + "        return method(GET).produces().consumes().as(new TypeToken<java.util.Optional<java.lang.String>>(Optional.class, new TypeToken<java.lang.String>(String.class){}){});\n"
                        + "    }\n"
                        + "}"));
    }
}
