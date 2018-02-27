package simplebutterknife.compiler;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import simplebutterknife.BindView;

@AutoService(Processor.class)
public class SimpleButterKnifeProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        Map<TypeElement, BindingSet> bindingMap = new LinkedHashMap<>();

        for (Element element : roundEnvironment.getElementsAnnotatedWith(BindView.class)) {
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            int id = element.getAnnotation(BindView.class).value();
            Name simpleName = element.getSimpleName();
            String name = simpleName.toString();
            TypeMirror elementType = element.asType();
            TypeName type = TypeName.get(elementType);

            BindingSet bindingSet = bindingMap.get(enclosingElement);

            if (bindingSet == null) {
                bindingSet = new BindingSet();
                TypeMirror typeMirror = enclosingElement.asType();
                TypeName targetType = TypeName.get(typeMirror);
                String packageName = MoreElements.getPackage(enclosingElement).getQualifiedName().toString();
                String className = enclosingElement.getQualifiedName().toString().substring(
                        packageName.length() + 1).replace('.', '$');
                ClassName bindingClassName = ClassName.get(packageName, className + "_ViewBinding");
                bindingSet.targetTypeName = targetType;
                ;
                bindingSet.bindingClassName = bindingClassName;
                bindingMap.put(enclosingElement, bindingSet);
            }

            if (bindingSet.viewBindings == null) {
                bindingSet.viewBindings = new ArrayList<>();
            }

            ViewBinding viewBinding = new ViewBinding();
            viewBinding.type = type;
            viewBinding.id = id;
            viewBinding.name = name;
            bindingSet.viewBindings.add(viewBinding);
        }

        for (Map.Entry<TypeElement, BindingSet> entry : bindingMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            BindingSet binding = entry.getValue();

            TypeName targetTypeName = binding.targetTypeName;
            ClassName bindingClassName = binding.bindingClassName;
            List<ViewBinding> viewBindings = binding.viewBindings;
            TypeSpec.Builder viewBindingBuilder = TypeSpec.classBuilder(bindingClassName.simpleName())
                    .addModifiers(Modifier.PUBLIC);
            viewBindingBuilder.addField(targetTypeName, "target", Modifier.PUBLIC);
            MethodSpec.Builder activityViewBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(targetTypeName, "target");
            activityViewBuilder.addStatement("this(target, target.getWindow().getDecorView())");
            viewBindingBuilder.addMethod(activityViewBuilder.build());
            MethodSpec.Builder viewBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(targetTypeName, "target")
                    .addParameter(ClassName.get("android.view", "View"), "source");
            viewBuilder.addStatement("this.target = target");
            viewBuilder.addCode("\n");
            for (ViewBinding viewBinding : viewBindings) {
                CodeBlock.Builder builder = CodeBlock.builder()
                        .add("target.$L = ", viewBinding.name);
                builder.add("($T) ", viewBinding.type);
                builder.add("source.findViewById($L)", CodeBlock.of("$L", viewBinding.id));
                viewBuilder.addStatement("$L", builder.build());
            }
            viewBindingBuilder.addMethod(viewBuilder.build());

            JavaFile javaFile = JavaFile.builder(bindingClassName.packageName(), viewBindingBuilder.build())
                    .build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }

        return false;
    }

    class BindingSet {
        TypeName targetTypeName;
        ClassName bindingClassName;
        List<ViewBinding> viewBindings;
    }

    class ViewBinding {
        TypeName type;
        int id;
        String name;
    }
}

