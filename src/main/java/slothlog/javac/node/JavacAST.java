/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.javac.node;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import slothlog.javac.transformer.JavacASTVisitor;


import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <p></p>
 * 自己定义的javac抽象语法树
 * @author lizhao 563868273@qq.com
 * @version lizhao: JavacAst.java, v 0.1 2018-07-14 下午3:54  $$
 */
public class JavacAST {
    /**
     * 整棵树的根节点,后续遍历需要依靠这个根节点
     */
    private JavacNode top;

    /**
     * 整棵树的记录Node是否重复的Map
     */
    private Map<JCTree, JCTree> handledMap = new IdentityHashMap();
    /**
     * 记录JCTree和javacNode关系的树Map,方面后续查找
     */
    private Map<JCTree, JavacNode> nodeMap = new IdentityHashMap<>();

    private final TreeMaker maker;

    private final JavacElements elements;

    private String fileName;

    private final Messager messager;

    private final Symtab symtab;

    private final JavacTypes javacTypes;
    /**
     * 指定方法体中哪些类型的Statement能被进入AST树
     */
    private final Collection<Class<? extends JCTree>> statementTypes;

    /**
     * 创建一个自己的JavacAST
     * @param messager
     * @param context
     * @param top
     */
    public JavacAST(Messager messager, Context context, JCTree.JCCompilationUnit top) {
        statementTypes = statementTypes();
        this.fileName = top.getSourceFile().toString();
        this.top = buildCompilationUnit(top);
        this.messager = messager;
        this.elements = JavacElements.instance(context);
        this.maker = TreeMaker.instance(context);
        this.symtab = Symtab.instance(context);
        this.javacTypes = JavacTypes.instance(context);
        this.fileName = top.sourcefile.toString();

    }

    /**
     * 指定方法体中哪些类型的Statement能被进入AST树
     * 如果需要扩展一些 可以这里自定义
     * @return
     */
    private static Collection<Class<? extends JCTree>> statementTypes() {
        Collection<Class<? extends JCTree>> collection = new ArrayList<>(3);
        collection.add(JCTree.JCStatement.class);
        collection.add(JCTree.JCExpression.class);
        collection.add(JCTree.JCCatch.class);
        return collection;
    }
    private JavacNode buildCompilationUnit(JCTree.JCCompilationUnit top) {
        java.util.List<JavacNode> children = new CopyOnWriteArrayList<>();
        //只取是类的
        for (JCTree def : top.defs){
            if (def instanceof JCTree.JCClassDecl){
                addIfNotNull(children, buildType((JCTree.JCClassDecl) def));
            }
        }
        return new JavacNode(this, top, children, NodeKind.COMPILATION_UNIT);
    }
    /**
     * 根据jcClassDecl构建出JavacNode
     * @param type
     * @return
     */
    private JavacNode buildType(JCTree.JCClassDecl type) {
        if (isHandled(type)){
            return null;
        }
        java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();
        //由于注解不在defs中，注解在type.mode.annotations中，需要单独处理
        for (JCTree.JCAnnotation annotation : type.mods.annotations){
            addIfNotNull(childNodes, buildAnnotation(annotation));
        }
        for (JCTree def: type.defs){
            //def中有四种类型 1.JCClassDecl 内部类 2.JCMethodDecl 方法 3.JCVariableDecl方法 4.JCBlock static方法块
            if (def instanceof JCTree.JCMethodDecl){
                addIfNotNull(childNodes, buildMethod((JCTree.JCMethodDecl) def));
            } else if (def instanceof JCTree.JCClassDecl) {
                addIfNotNull(childNodes, buildType((JCTree.JCClassDecl) def));
            } else if (def instanceof JCTree.JCVariableDecl){
                addIfNotNull(childNodes, buildField((JCTree.JCVariableDecl) def));
            }else if (def instanceof JCTree.JCBlock){
                addIfNotNull(childNodes, buildInitializer((JCTree.JCBlock) def));
            }
        }
        return putInMap(new JavacNode(this, type, childNodes, NodeKind.TYPE));
    }

    /**
     * 构建static的JavacNode
     * @param
     * @return
     */
    private JavacNode buildInitializer(JCTree.JCBlock initializer) {
        if (isHandled(initializer)) {
            return null;
        }
        java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();

        for (JCTree.JCStatement statement: initializer.stats) {
            //对static块里面的statement进行操作
            addIfNotNull(childNodes, buildStatement(statement));
        }
        return putInMap(new JavacNode(this, initializer, childNodes, NodeKind.INITIALIZER));
    }

    private JavacNode putInMap(JavacNode javacNode) {
        handledMap.put(javacNode.getNode(), javacNode.getNode());
        nodeMap.put(javacNode.getNode(), javacNode);
        return javacNode;
    }

    /**
     * 根据field构建出JavacNode
     * @param field
     * @return
     */
    private JavacNode buildField(JCTree.JCVariableDecl field) {
        if (isHandled(field)) {
            return null;
        }
        java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();
        for (JCTree.JCAnnotation annotation : field.mods.annotations) {
            addIfNotNull(childNodes, buildAnnotation(annotation));
        }
        addIfNotNull(childNodes, buildExpression(field.init));
        return putInMap(new JavacNode(this, field, childNodes, NodeKind.FIELD));
    }


    /**
     * 根据method构建出JavacNode
     * @param method
     * @return
     */
    private JavacNode buildMethod(JCTree.JCMethodDecl method) {
        if (isHandled(method)){
            return null;
        }
        java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();
        for (JCTree.JCAnnotation annotation : method.mods.annotations) {
            addIfNotNull(childNodes, buildAnnotation(annotation));
        }
        for (JCTree.JCVariableDecl param : method.params)
        {
            addIfNotNull(childNodes, buildLocalVar(param, NodeKind.ARGUMENT));
        }
        if (method.body != null && method.body.stats != null) {
            for (JCTree.JCStatement statement : method.body.stats) {
                addIfNotNull(childNodes, buildStatement(statement));
            }
        }
        return putInMap(new JavacNode(this, method, childNodes, NodeKind.METHOD));
    }

    /**
     * 构建局部变量
     * @param local
     * @param kind 有两种一种是方法上的参数 NodeKind.ARGUMENT, 还有一种是方法里面的变量NodeKind.LOCAL
     * @return
     */
    private JavacNode buildLocalVar(JCTree.JCVariableDecl local, NodeKind kind) {
        if (isHandled(local)){
            return null;
        }
        java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();
        for (JCTree.JCAnnotation annotation : local.mods.annotations) {
            addIfNotNull(childNodes, buildAnnotation(annotation));
        }
        //将初始化表达式 构建成子节点
        addIfNotNull(childNodes, buildExpression(local.init));
        return putInMap(new JavacNode(this, local, childNodes, kind));
    }

    private JavacNode buildExpression(JCTree.JCExpression expression) {
        return  buildStatementOrExpression(expression);
    }

    private JavacNode buildStatement(JCTree.JCStatement statement) {
        return buildStatementOrExpression(statement);
    }

    /**
     * 这里构建语句比较复杂 直接用的Lombok中的，但是如果需要修改方法里面的就可以修改这里
     * @param statement
     * @return
     */
    private JavacNode buildStatementOrExpression(JCTree statement) {
        if (statement == null) {
            return null;
        }
        if (statement instanceof JCTree.JCAnnotation) {
            return null;
        }
        if (statement instanceof JCTree.JCClassDecl) {
            return buildType((JCTree.JCClassDecl)statement);
        }
        if (statement instanceof JCTree.JCVariableDecl) {
            return buildLocalVar((JCTree.JCVariableDecl)statement, NodeKind.LOCAL);
        }
        if (statement instanceof JCTree.JCReturn){
            return buildReturn((JCTree.JCReturn)statement, NodeKind.RERTUN);
        }
        if (statement instanceof JCTree.JCTry) {
            return buildTry((JCTree.JCTry) statement);
        }
        if (statement.getClass().getSimpleName().equals("JCLambda")) {
            return buildLambda((JCTree.JCLambda) statement);
        }
        if (isHandled(statement)) return null;

        return drill(statement);
    }

    private JavacNode buildReturn(JCTree.JCReturn jcReturn, NodeKind kind) {
        if (isHandled(jcReturn)){
            return null;
        }
        return putInMap(new JavacNode(this, jcReturn, null, kind));
    }

    private JavacNode drill(JCTree statement) {
        try {
            java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();
            for (FieldAccess fa : fieldsOf(statement.getClass())) childNodes.addAll(buildWithField(JavacNode.class, statement, fa));
            return putInMap(new JavacNode(this, statement, childNodes, NodeKind.STATEMENT));
        } catch (OutOfMemoryError oome) {
            String msg = oome.getMessage();
            if (msg == null) msg = "(no original message)";
            OutOfMemoryError newError = new OutOfMemoryError(getFileName() + "@pos" + statement.getPreferredPosition() + ": " + msg);
            // We could try to set the stack trace of the new exception to the same one as the old exception, but this costs memory,
            // and we're already in an extremely fragile situation in regards to remaining heap space, so let's not do that.
            throw newError;
        }
    }
    /**
     * buildTree implementation that uses reflection to find all child nodes by way of inspecting
     * the fields. */
    protected Collection<JavacNode> buildWithField(Class<JavacNode> nodeType, JCTree statement, FieldAccess fa) {
        java.util.List<JavacNode> list = new ArrayList<>();
        buildWithField0(nodeType, statement, fa, list);
        return list;
    }

    @SuppressWarnings("unchecked")
    private void buildWithField0(Class<JavacNode> nodeType, JCTree child, FieldAccess fa, java.util.List<JavacNode> list) {
        try {
            Object o = fa.field.get(child);
            if (o == null) return;
            if (fa.dim == 0) {
                JavacNode node = buildTree((JCTree) o, NodeKind.STATEMENT);
                if (node != null) list.add(nodeType.cast(node));
            } else if (o.getClass().isArray()) {
                buildWithArray(nodeType, o, list, fa.dim);
            } else if (Collection.class.isInstance(o)) {
                buildWithCollection(nodeType, o, list, fa.dim);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    @SuppressWarnings("unchecked")
    private void buildWithArray(Class<JavacNode> nodeType, Object array, java.util.List<JavacNode> list, int dim) {
        if (dim == 1) {
            for (Object v : (Object[])array) {
                if (v == null) continue;
                JavacNode node = buildTree((JCTree)v, NodeKind.STATEMENT);
                if (node != null) list.add(nodeType.cast(node));
            }
        } else for (Object v : (Object[])array) {
            if (v == null) return;
            buildWithArray(nodeType, v, list, dim -1);
        }
    }

    @SuppressWarnings("unchecked")
    private void buildWithCollection(Class<JavacNode> nodeType, Object collection, java.util.List<JavacNode> list, int dim) {
        if (dim == 1) {
            for (Object v : (Collection<?>)collection) {
                if (v == null) continue;
                JavacNode node = buildTree((JCTree)v, NodeKind.STATEMENT);
                if (node != null) list.add(nodeType.cast(node));
            }
        } else for (Object v : (Collection<?>)collection) {
            buildWithCollection(nodeType, v, list, dim-1);
        }
    }
    /**
     * lambda表达式
     * @param jcTree
     * @return
     */
    private JavacNode buildLambda(JCTree.JCLambda jcTree) {

        return buildStatementOrExpression(jcTree.body);
    }

    private JCTree getBody(JCTree jcTree) {
        try {
            Method method = getBodyMethod(jcTree.getClass());
            if (method == null){
                return null;
            }
            return (JCTree) method.invoke(jcTree);
        } catch (Exception e) {
           messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
           return null;
        }
    }
    private final static ConcurrentMap<Class<?>, Method> getBodyMethods = new ConcurrentHashMap<Class<?>, Method>();

    private Method getBodyMethod(Class<?> c) {
        Method m = getBodyMethods.get(c);
        if (m != null) {
            return m;
        }
        try {
            m = c.getMethod("getBody");
        } catch (NoSuchMethodException e) {
             messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return getBodyMethods.putIfAbsent(c, m);
    }

    private JavacNode buildTry(JCTree.JCTry tryNode) {
        if (isHandled(tryNode)) {
            return null;
        }
        java.util.List<JavacNode> childNodes = new CopyOnWriteArrayList<>();
        //try资源
        for (JCTree varDecl : getResourcesForTryNode(tryNode)) {
            if (varDecl instanceof JCTree.JCVariableDecl) {
                addIfNotNull(childNodes, buildLocalVar((JCTree.JCVariableDecl) varDecl, NodeKind.LOCAL));
            }
        }
        //try的方法体
        addIfNotNull(childNodes, buildStatement(tryNode.body));
        //catch
        for (JCTree.JCCatch jcc : tryNode.catchers) {
            addIfNotNull(childNodes, buildTree(jcc, NodeKind.STATEMENT));
        }
        //finally
        addIfNotNull(childNodes, buildStatement(tryNode.finalizer));
        return putInMap(new JavacNode(this, tryNode, childNodes, NodeKind.STATEMENT));
    }

    /**
     * 根据JCAnnotation构建出JavacNode
     * @param annotation
     * @return
     */
    private JavacNode buildAnnotation(JCTree.JCAnnotation annotation) {
        if (isHandled(annotation)){
            return null;
        }
        return putInMap(new JavacNode(this, annotation, null, NodeKind.ANNOTATION));
    }

    /**
     * 判断是否被处理
     * @param node
     * @return
     */
    private boolean isHandled(JCTree node) {
        return handledMap.put(node, node) != null;
    }

    private void addIfNotNull(java.util.List<JavacNode> chirldren, JavacNode javacNode) {
        if (javacNode != null){
            chirldren.add(javacNode);
        }
    }
    public JavacNode buildTree(JCTree node, NodeKind kind) {
        switch (kind) {
            case COMPILATION_UNIT:
                return buildCompilationUnit((JCTree.JCCompilationUnit) node);
            case TYPE:
                return buildType((JCTree.JCClassDecl) node);
            case FIELD:
                return buildField((JCTree.JCVariableDecl) node);
            case METHOD:
                return buildMethod((JCTree.JCMethodDecl) node);
            case ARGUMENT:
                return buildLocalVar((JCTree.JCVariableDecl) node, kind);
            case LOCAL:
                return buildLocalVar((JCTree.JCVariableDecl) node, kind);
            case STATEMENT:
                return buildStatementOrExpression(node);
            case ANNOTATION:
                return buildAnnotation((JCTree.JCAnnotation) node);
            default:
                throw new AssertionError("Did not expect: " + kind);
        }
    }
    private static boolean JCTRY_RESOURCES_FIELD_INITIALIZED;
    private static Field JCTRY_RESOURCES_FIELD;

    /**
     * 如果传的是
     * try (BufferedReader br = new BufferedReader(new FileReader(""))) {
     *      br.readLine();
     *       }
     *       则resource就是BufferedReader br = new BufferedReader(new FileReader("")
     * @param tryNode
     * @return
     */
    private static java.util.List<JCTree> getResourcesForTryNode(JCTree.JCTry tryNode) {
        if (!JCTRY_RESOURCES_FIELD_INITIALIZED) {
            try {
                JCTRY_RESOURCES_FIELD = JCTree.JCTry.class.getField("resources");
            } catch (NoSuchFieldException ignore) {
                // Java 1.6 or lower won't have this at all.
            } catch (Exception ignore) {
                // Shouldn't happen. Best thing we can do is just carry on and break on try/catch.
            }
            JCTRY_RESOURCES_FIELD_INITIALIZED = true;
        }

        if (JCTRY_RESOURCES_FIELD == null) return Collections.emptyList();
        Object rv = null;
        try {
            rv = JCTRY_RESOURCES_FIELD.get(tryNode);
        } catch (Exception ignore) {}

        if (rv instanceof java.util.List) return (java.util.List<JCTree>) rv;
        return Collections.emptyList();
    }

    public void visit(JavacASTVisitor visitor) {
        top.visit(visitor);
    }

    public void traverseChildren(JavacASTVisitor visitor, JavacNode javacNode) {
        for (JavacNode child : javacNode.getChildren()){
            child.visit(visitor);
        }
    }

    /**
     * Represents a field that contains AST children.
     */
    protected static class FieldAccess {
        /** The actual field. */
        public final Field field;
        /** Dimensions of the field. Works for arrays, or for java.util.collections. */
        public final int dim;

        FieldAccess(Field field, int dim) {
            this.field = field;
            this.dim = dim;
        }
    }

    private static final ConcurrentMap<Class<?>, Collection<FieldAccess>> fieldsOfASTClasses = new ConcurrentHashMap<Class<?>, Collection<FieldAccess>>();

    /** Returns FieldAccess objects for the stated class. Each field that contains objects of the kind returned by
     * {@link #getStatementTypes()}, either directly or inside of an array or java.util.collection (or array-of-arrays,
     * or collection-of-collections, et cetera), is returned.
     */
    protected Collection<FieldAccess> fieldsOf(Class<?> c) {
        Collection<FieldAccess> fields = fieldsOfASTClasses.get(c);
        if (fields != null) return fields;

        fields = new ArrayList<FieldAccess>();
        getFields(c, fields);
        fieldsOfASTClasses.putIfAbsent(c, fields);
        return fieldsOfASTClasses.get(c);
    }

    private void getFields(Class<?> c, Collection<FieldAccess> fields) {
        if (c == Object.class || c == null) return;
        for (Field field : c.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Class<?> filedClass = field.getType();
            int dim = 0;
            //判断是否是array或者 Collection
            if (filedClass.isArray()) {
                while (filedClass.isArray()) {
                    dim++;
                    filedClass = filedClass.getComponentType();
                }
            } else {
                while (Collection.class.isAssignableFrom(filedClass)) {
                    dim++;
                    filedClass = getComponentType(field.getGenericType());
                }
            }

            if (shouldDrill(filedClass)) {
                field.setAccessible(true);
                fields.add(new FieldAccess(field, dim));
            }
        }

        getFields(c.getSuperclass(), fields);
    }

    private Class<?> getComponentType(Type type) {
        if (type instanceof ParameterizedType) {
            Type component = ((ParameterizedType)type).getActualTypeArguments()[0];
            return component instanceof Class<?> ? (Class<?>)component : Object.class;
        }
        return Object.class;
    }

    private boolean shouldDrill( Class<?> filedClass) {
        for (Class<?> statementType : statementTypes) {
            if (statementType.isAssignableFrom(filedClass)) return true;
        }

        return false;
    }

    public JavacNode getTop() {
        return top;
    }

    public TreeMaker getMaker() {
        return maker;
    }

    public JavacElements getElements() {
        return elements;
    }

    public String getFileName() {
        return fileName;
    }

    public Messager getMessager() {
        return messager;
    }

    public Symtab getSymtab() {
        return symtab;
    }

    public JavacTypes getJavacTypes() {
        return javacTypes;
    }
}