/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.javac.apt;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import slothlog.javac.transformer.JavacTransformer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

/**
 * <p></p>
 *
 * @author lizhao
 * @version $lizhao07: SlothProcessor.java
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SlothProcessor extends AbstractProcessor {
    private Trees trees;
    private JavacTransformer transformer;
    private Messager messager;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.messager = processingEnv.getMessager();
        this.transformer = new JavacTransformer(processingEnv);

    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //判断是否是最后一轮，最后一轮直接返回 或 处于关闭状态
        if (roundEnv.processingOver() || isOFF()) {
            return false;
        }
        //获取所有根elements
        Set<? extends Element> rootElements = roundEnv.getRootElements();
        //所有根树
        java.util.List<JCTree.JCCompilationUnit> jcRootTrees = new ArrayList<>();
        //将所有element节点转换为树
        for (Element rootElement : rootElements) {
            jcRootTrees.add((JCTree.JCCompilationUnit) trees.getPath(rootElement).getCompilationUnit());
        }
        this.transformer.transfrom(jcRootTrees);
        return true;
    }

    private boolean isOFF() {
        try {
            Properties prop = new Properties();

            prop.load(this.getClass().getResourceAsStream("/slothlog.properties"));
            String state = (String) prop.get("state");
            if (state == null || !"OFF".equals(state)){
                return false;
            }
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.WARNING, e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }

}