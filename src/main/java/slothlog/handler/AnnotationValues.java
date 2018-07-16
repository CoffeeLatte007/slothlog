/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler;

import slothlog.javac.node.JavacNode;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: AnnotationValues.java, v 0.1 2018-07-14 下午11:43  $$
 */
public class AnnotationValues<A extends Annotation> {
    private final Class<A> type;
    private final Map<String, AnnotationValue> values;
    private final JavacNode javacNode;

    public AnnotationValues(Class<A> type, Map<String, AnnotationValue> values, JavacNode javacNode) {
        this.type = type;
        this.values = values;
        this.javacNode = javacNode;
    }
}