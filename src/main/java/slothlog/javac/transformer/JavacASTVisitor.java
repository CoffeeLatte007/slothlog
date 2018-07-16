/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.javac.transformer;

import com.sun.tools.javac.tree.JCTree;
import slothlog.javac.node.JavacNode;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: JavacASTVisitor.java, v 0.1 2018-07-14 下午8:03  $$
 */
public interface JavacASTVisitor {
    public void visitAnnotation(JavacNode annotationNode, JCTree.JCAnnotation annotation);
}