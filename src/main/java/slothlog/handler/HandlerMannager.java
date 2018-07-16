/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler;

import com.sun.tools.javac.tree.JCTree;
import slothlog.annotation.LogInfo;
import slothlog.handler.log.LogInfoHandler;
import slothlog.javac.node.JavacNode;

import javax.annotation.processing.Messager;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: HandlerMannager.java, v 0.1 2018-07-13 下午6:21  $$
 */
public class HandlerMannager {


    private Messager messager;

    private static Map<String, AnnotationHandlerContainer> handlerMap = new HashMap<>();
    static {
        handlerMap.put("slothlog.annotation.LogInfo", new AnnotationHandlerContainer(new LogInfoHandler(), LogInfo.class));
    }

    public HandlerMannager(Messager messager) {
        this.messager = messager;
    }


    public void handleAnnotation(JCTree.JCCompilationUnit top, JavacNode annotationNode, JCTree.JCAnnotation annotation) {
        if (annotation == null || annotation.annotationType == null){
            return;
        }
        if (annotation.type != null || annotation.annotationType.toString().contains(".")){
            String annotationtClassName = null;
            if (annotation.type != null){
                annotationtClassName = annotation.type.toString();
            }else {
                annotationtClassName = annotation.annotationType.toString();
            }
            if (annotationtClassName == null){
                return ;
            }
            //从Map中去取，没有就直接返回
            AnnotationHandlerContainer handlerContainer = handlerMap.get(annotationtClassName);
            if (handlerContainer == null){
                return;
            }
            handlerContainer.handle(annotationNode);
        }else {
            List<String> annotationtClassNames = getNameFromSimpleName(top, annotation.annotationType.toString());
            for (String annotationtClassName : annotationtClassNames) {
                AnnotationHandlerContainer handlerContainer = handlerMap.get(annotationtClassName);
                if (handlerContainer == null) {
                    continue;
                }
                handlerContainer.handle(annotationNode);
                return;
            }
        }

    }

    private List<String> getNameFromSimpleName(JCTree.JCCompilationUnit top, String simpleName) {
        List<String> fullNames = new ArrayList<>();
        for (JCTree def : top.defs) {
            if (!(def instanceof JCTree.JCImport)) {
                continue;
            }
            JCTree qual = ((JCTree.JCImport) def).qualid;
            if (!(qual instanceof JCTree.JCFieldAccess)) {
                continue;
            }
            String qulSimpleName = ((JCTree.JCFieldAccess) qual).name.toString();
            if (simpleName.equals(qulSimpleName)){
                fullNames.add(qual.toString());
            }
        }
        return fullNames;
    }

    private static class AnnotationHandlerContainer <T extends Annotation>{
        private final AnnotationHandler<T> handler;
        private final Class<T> annotationClass;

        AnnotationHandlerContainer(AnnotationHandler<T> handler, Class<T> annotationClass) {
            this.handler = handler;
            this.annotationClass = annotationClass;

        }
        public void handle(final JavacNode node) {
            //这里需要获取annotation的value值
            handler.handle(JavacHandlerUtil.createAnnotation(annotationClass, node), node);
        }


    }
}