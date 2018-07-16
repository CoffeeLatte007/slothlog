/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler;

import slothlog.javac.node.JavacNode;

import java.lang.annotation.Annotation;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: AnnotationHandler.java, v 0.1 2018-07-13 下午6:06  $$
 */
public interface AnnotationHandler<T extends Annotation> {

    void handle(AnnotationValues<T> annotation, JavacNode node);
}