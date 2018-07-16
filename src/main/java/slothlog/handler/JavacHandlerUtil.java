/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import slothlog.javac.node.JavacNode;
import slothlog.javac.node.NodeKind;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.tools.javac.code.Flags.GENERATEDCONSTR;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: JavacHandlerUtil.java, v 0.1 2018-07-14 下午9:16  $$
 */
public class JavacHandlerUtil {


    /**
     * Creates an instance of {@code AnnotationValues} for the provided AST Node.
     *
     * @param type An annotation class type, such as {@code lombok.Getter.class}.
     * @param node A Lombok AST node representing an annotation in source code.
     */
    public static <A extends Annotation> AnnotationValues<A> createAnnotation(Class<A> type, final JavacNode node) {
        return createAnnotation(type, (JCTree.JCAnnotation) node.getNode(), node);
    }
    /**
     * Creates an instance of {@code AnnotationValues} for the provided AST Node
     * and Annotation expression.
     *
     * @param type An annotation class type, such as {@code lombok.Getter.class}.
     * @param anno the annotation expression
     * @param node A Lombok AST node representing an annotation in source code.
     */
    public static <A extends Annotation> AnnotationValues<A> createAnnotation(Class<A> type, JCTree.JCAnnotation anno, final JavacNode node) {
        Map<String, AnnotationValue> values = new HashMap<String, AnnotationValue>();
        List<JCTree.JCExpression> arguments = anno.getArguments();

        for (JCTree.JCExpression arg : arguments) {
            String mName;
            JCTree.JCExpression rhs;
            java.util.List<String> raws = new ArrayList<String>();
            java.util.List<Object> expressions = new ArrayList<Object>();
            final java.util.List<JCDiagnostic.DiagnosticPosition> positions = new ArrayList<JCDiagnostic.DiagnosticPosition>();

            if (arg instanceof JCTree.JCAssign) {
                JCTree.JCAssign assign = (JCTree.JCAssign) arg;
                mName = assign.lhs.toString();
                rhs = assign.rhs;
            } else {
                rhs = arg;
                mName = "value";
            }

            if (rhs instanceof JCTree.JCNewArray) {
                List<JCTree.JCExpression> elems = ((JCTree.JCNewArray) rhs).elems;
                for (JCTree.JCExpression inner : elems) {
                    raws.add(inner.toString());
                    expressions.add(inner);

                    positions.add(inner.pos());
                }
            } else {
                raws.add(rhs.toString());
                expressions.add(rhs);
                positions.add(rhs.pos());
            }

            values.put(mName, new AnnotationValue(node, raws, expressions) {

            });
        }

        return new AnnotationValues<A>(type, values, node);
    }
    /**
     * Checks if there is a field with the provided name.
     *
     * @param fieldName the field name to check for.
     * @param node Any node that represents the Type (JCClassDecl) to look in, or any child node thereof.
     */
    public static JCTree.JCVariableDecl fieldExists(String fieldName, JavacNode node) {
        node = upToTypeNode(node);

        if (node != null && node.getNode() instanceof JCTree.JCClassDecl) {
            for (JCTree def : ((JCTree.JCClassDecl)node.getNode()).defs) {
                if (def instanceof JCTree.JCVariableDecl) {
                    if (((JCTree.JCVariableDecl)def).name.contentEquals(fieldName)) {
                        return (JCTree.JCVariableDecl)def;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 找到这个node最近的class
     * @param node
     * @return
     */
    public static JavacNode upToTypeNode(JavacNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        while ((node != null) && !(node.getNode() instanceof JCTree.JCClassDecl)) {
            node = node.getParent();
        }

        return node;
    }
    /**
     * 找到这个node最近的class
     * @param node
     * @return
     */
    public static JavacNode upToMethodNode(JavacNode node) {
        if (node == null) {
            throw new NullPointerException("node");
        }
        while ((node != null) && !(node.getNode() instanceof JCTree.JCMethodDecl)) {
            node = node.getParent();
        }

        return node;
    }

    public static JCTree.JCFieldAccess getSelfType(JavacNode typeNode) {
        TreeMaker maker = typeNode.getTreeMaker();
        Name name = ((JCTree.JCClassDecl) typeNode.getNode()).name;
        return maker.Select(maker.Ident(name), typeNode.toName("class"));
    }

    /**
     * In javac, dotted access of any kind, from {@code java.lang.String} to {@code var.methodName}
     * is represented by a fold-left of {@code Select} nodes with the leftmost string represented by
     * a {@code Ident} node. This method generates such an expression.
     *
     * For example, maker.Select(maker.Select(maker.Ident(NAME[java]), NAME[lang]), NAME[String]).
     *
     * @see com.sun.tools.javac.tree.JCTree.JCIdent
     * @see com.sun.tools.javac.tree.JCTree.JCFieldAccess
     */
    public static JCTree.JCExpression chainDotsString(JavacNode node, String elems) {
        return chainDots(node, null, null, elems.split("\\."));
    }
    /**
     * In javac, dotted access of any kind, from {@code java.lang.String} to {@code var.methodName}
     * is represented by a fold-left of {@code Select} nodes with the leftmost string represented by
     * a {@code Ident} node. This method generates such an expression.
     * <p>
     * The position of the generated node(s) will be unpositioned (-1).
     *
     * For example, maker.Select(maker.Select(maker.Ident(NAME[java]), NAME[lang]), NAME[String]).
     *
     * @see com.sun.tools.javac.tree.JCTree.JCIdent
     * @see com.sun.tools.javac.tree.JCTree.JCFieldAccess
     */
    public static JCTree.JCExpression chainDots(JavacNode node, String elem1, String elem2, String... elems) {
        return chainDots(node, -1, elem1, elem2, elems);
    }

    /**
     * In javac, dotted access of any kind, from {@code java.lang.String} to {@code var.methodName}
     * is represented by a fold-left of {@code Select} nodes with the leftmost string represented by
     * a {@code Ident} node. This method generates such an expression.
     * <p>
     * The position of the generated node(s) will be equal to the {@code pos} parameter.
     *
     * For example, maker.Select(maker.Select(maker.Ident(NAME[java]), NAME[lang]), NAME[String]).
     *
     * @see com.sun.tools.javac.tree.JCTree.JCIdent
     * @see com.sun.tools.javac.tree.JCTree.JCFieldAccess
     */
    public static JCTree.JCExpression chainDots(JavacNode node, int pos, String elem1, String elem2, String... elems) {
        assert elems != null;

        TreeMaker maker = node.getTreeMaker();
        if (pos != -1) maker = maker.at(pos);
        JCTree.JCExpression e = null;
        if (elem1 != null) e = maker.Ident(node.toName(elem1));
        if (elem2 != null) e = e == null ? maker.Ident(node.toName(elem2)) : maker.Select(e, node.toName(elem2));
        for (int i = 0 ; i < elems.length ; i++) {
            e = e == null ? maker.Ident(node.toName(elems[i])) : maker.Select(e, node.toName(elems[i]));
        }

        assert e != null;

        return e;
    }

    /**
     * 插入field
     * 需要插在类变量前面即可
     * @param typeNode
     * @param field
     * @return
     */
    public static JavacNode injectField(JavacNode typeNode, JCTree.JCVariableDecl field) {
        JCTree.JCClassDecl type = (JCTree.JCClassDecl) typeNode.getNode();
        List<JCTree> insertAfter = null;
        List<JCTree> insertBefore = type.defs;
        while (true) {
            boolean skip = false;
            if (insertBefore.head instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl f = (JCTree.JCVariableDecl) insertBefore.head;
                if (isEnumConstant(f) ) {
                    skip = true;
                }
            } else if (insertBefore.head instanceof JCTree.JCMethodDecl) {
                if ((((JCTree.JCMethodDecl) insertBefore.head).mods.flags & GENERATEDCONSTR) != 0) {
                    skip = true;
                }
            }
            if (skip) {
                insertAfter = insertBefore;
                insertBefore = insertBefore.tail;
                continue;
            }
            break;
        }
        List<JCTree> fieldEntry = List.<JCTree>of(field);
        fieldEntry.tail = insertBefore;
        if (insertAfter == null) {
            type.defs = fieldEntry;
        } else {
            insertAfter.tail = fieldEntry;
        }

        return typeNode.add(field, NodeKind.FIELD);
    }
    public static boolean isEnumConstant(final JCTree.JCVariableDecl field) {
        return (field.mods.flags & Flags.ENUM) != 0;
    }

    public static java.util.List<JavacNode> getParamsNode(JavacNode methodNode) {
        if (methodNode.getKind() != NodeKind.METHOD){
            return Collections.emptyList();
        }
        java.util.List<JavacNode> nodes = new ArrayList<>();
        for (JavacNode javacNode: methodNode.getChildren()){
            if (javacNode.getKind() == NodeKind.ARGUMENT){
                nodes.add(javacNode);
            }
        }
        return nodes;
    }

    public static String getChildDisVariableName( JavacNode methodNode, String variableName) {
        Set<String> variableNames = new HashSet<>();
        for (JavacNode child: methodNode.getChildren()) {
            if (child.getKind() == NodeKind.LOCAL){
                variableNames.add(((JCTree.JCVariableDecl)child.getNode()).name.toString());
            }
        }
        int i = 0;
        String result = variableName;
        while (variableNames.contains(result)){
            result = variableName + String.valueOf(i);
        }
        return result;
    }

    public static boolean hasAnnotation(JavacNode node, Class<?> annotationClass) {
        for (JavacNode child : node.getChildren()){
            if (child.getKind() == NodeKind.ANNOTATION){
                JCTree.JCAnnotation annotationNode = ((JCTree.JCAnnotation)child.getNode());
                if (annotationNode.annotationType.type == null && annotationNode.annotationType.toString().equals(annotationClass.getName())){
                    return true;
                }
                if (annotationNode.annotationType.type != null && annotationNode.annotationType.type.toString().equals(annotationClass.getName())){
                    return true;
                }
            }
        }
        return false;
    }

    public static String getTypeName(JavacNode node) {
        JavacNode typeNode = upToTypeNode(node);
        if (typeNode == null){
            return null;
        }
        return ((JCTree.JCClassDecl)typeNode.getNode()).name.toString();
    }
}
