/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.javac.transformer;

import com.sun.source.util.Trees;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import slothlog.handler.HandlerMannager;
import slothlog.javac.node.JavacAST;
import slothlog.javac.node.JavacNode;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.util.ArrayList;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: JavacTransformer.java, v 0.1 2018-07-14 下午1:50  $$
 */
public class JavacTransformer {

    private Messager messager;
    private Context context;
    private Trees trees;
    private TreeMaker maker;
    private Names names;
    private JavacElements elements;
    private HandlerMannager handlerMannager;
    public JavacTransformer(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
        this.trees = Trees.instance(processingEnv);
        this.context = ((JavacProcessingEnvironment)processingEnv).getContext();
        this.maker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        this.elements = (JavacElements) processingEnv.getElementUtils();
        this.handlerMannager = new HandlerMannager(messager);
    }

    /**
     * 将注解处理器中传来的根节点树，通过修改AST，生成最终的树
     * @param jcRootTrees
     */
    public void transfrom(java.util.List<JCTree.JCCompilationUnit> jcRootTrees) {
        //将javac的树构建成我们自己的定义的树方便后续操作
        java.util.List<JavacAST> javacASTS = new ArrayList<>();
        for (JCTree.JCCompilationUnit jcCompilationUnit: jcRootTrees) {
            javacASTS.add(new JavacAST(messager, context, jcCompilationUnit));
        }
        for (JavacAST javaAST: javacASTS) {
            javaAST.visit(new SimpleAnnotationVisitor());
        }
    }
    private class SimpleAnnotationVisitor implements JavacASTVisitor {
        @Override
        public void visitAnnotation(JavacNode annotationNode, JCTree.JCAnnotation annotation){
            JCTree.JCCompilationUnit top = (JCTree.JCCompilationUnit) annotationNode.getTop().getNode();
            handlerMannager.handleAnnotation(top, annotationNode, annotation);
        }
    }

}