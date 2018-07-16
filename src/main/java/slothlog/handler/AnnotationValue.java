/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler;

import slothlog.javac.node.JavacNode;

import java.util.List;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: AnnotationValue.java, v 0.1 2018-07-14 下午11:38  $$
 */
public class AnnotationValue {
    private final JavacNode node;
    /** A list of the raw expressions. List is size 1 unless an array is provided. */
    private final List<String> raws;
    /** A list of the actual expressions. List is size 1 unless an array is provided. */
    private final List<Object> expressions;


    public AnnotationValue(JavacNode node, List<String> raws, List<Object> expressions) {
        this.node = node;
        this.raws = raws;
        this.expressions = expressions;
    }
}