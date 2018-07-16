/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.javac.node;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import slothlog.javac.transformer.JavacASTVisitor;


import java.util.concurrent.CopyOnWriteArrayList;


/**
 * <p></p>
 * 将JavacTree节点抽象成我们自己的树的节点
 * @author lizhao 563868273@qq.com
 * @version lizhao: JavacNode.java, v 0.1 2018-07-14 下午2:01  $$
 */
public class JavacNode {
    /**
     * 父节点 只有一个
     */
    private JavacNode parent;
    /**
     * 子节点多个
     * 子节点包括所有的defs,annotations。
     * 如果是方法得包括所有的statement
     */
    private java.util.List<JavacNode> children;
    /**
     * 当前JavacNode对应
     */
    private JCTree node;


    private JavacAST javacAST;


    private NodeKind kind;

    public JavacNode(JavacAST javacAST, JCTree jcTree, java.util.List<JavacNode> childNodes, NodeKind kind) {
        this.node = jcTree;
        this.children = childNodes != null ? childNodes : new CopyOnWriteArrayList<>();
        this.javacAST = javacAST;
        this.kind = kind;
        //建立父子关系
        for (JavacNode child : children){
            child.setParent(this);
        }
    }

    private void setParent(JavacNode javacNode) {
        this.parent = javacNode;
    }

    public JCTree getNode() {
        return node;
    }

    /**
     * 获取树的顶部
     * @return
     */
    public JavacNode getTop() {
        return javacAST.getTop();
    }

    /**
     * 除注解外的其他所有都直接递归遍历子树
     * 注解的需要进行处理
     * @param visitor
     */
    public void visit(JavacASTVisitor visitor) {
        switch (this.getKind()) {
            case COMPILATION_UNIT:
                javacAST.traverseChildren(visitor, this);
                break;
            case TYPE:
                javacAST.traverseChildren(visitor, this);
                break;
            case FIELD:
                javacAST.traverseChildren(visitor, this);
                break;
            case METHOD:
                javacAST.traverseChildren(visitor, this);
                break;
            case INITIALIZER:
                javacAST.traverseChildren(visitor, this);
                break;
            case ARGUMENT:
                javacAST.traverseChildren(visitor, this);
                break;
            case LOCAL:
                javacAST.traverseChildren(visitor, this);
                break;
            case STATEMENT:
                javacAST.traverseChildren(visitor, this);
                break;
            case ANNOTATION:
               visitor.visitAnnotation(this, (JCTree.JCAnnotation)getNode());
                break;
            case RERTUN:
                javacAST.traverseChildren(visitor, this);
                break;
            default:
                throw new AssertionError("Unexpected kind during node traversal: " + getKind());
        }
    }

    public NodeKind getKind() {
        return kind;
    }

    public java.util.List<JavacNode> getChildren() {
        return children;
    }

    public JavacNode getParent() {
        return parent;
    }

    public JavacAST getJavacAST() {
        return javacAST;
    }

    public TreeMaker getTreeMaker() {
        return javacAST.getMaker();
    }

    public Name toName(String name) {
        return javacAST.getElements().getName(name);
    }

    public JavacNode add(JCTree field, NodeKind kind) {
        JavacNode node = javacAST.buildTree(field, kind);
        if (node == null){
            return null;
        }
        node.parent = this;
        children.add(node);
        return node;
    }
}