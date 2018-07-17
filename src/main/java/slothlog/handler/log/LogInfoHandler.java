/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler.log;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import slothlog.annotation.LogInfo;
import slothlog.handler.AnnotationHandler;
import slothlog.handler.AnnotationValues;
import slothlog.handler.JavacHandlerUtil;
import slothlog.javac.node.JavacNode;
import slothlog.javac.node.NodeKind;



/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: LogInfoHandler.java, v 0.1 2018-07-13 下午6:01  $$
 */
public class LogInfoHandler implements AnnotationHandler<LogInfo> {
    public static final String LOG_FIELD_NAME = "LOGGER";
    public static final String LOG_FULL_CLASS_NAME = "org.slf4j.Logger";

    @Override
    public void handle(AnnotationValues<LogInfo> annotation, JavacNode annotationNode) {
        /**
         * 不处理接口
         */
        if (JavacHandlerUtil.isInterface(JavacHandlerUtil.upToTypeNode(annotationNode))){
            return;
        }
        JavacNode node = annotationNode.getParent();
        switch (node.getKind()){
            case TYPE:
                handleType(annotation, annotationNode, node);
                break;
            case METHOD:
                handleMethod(annotation, annotationNode, node);
                break;
            case LOCAL:
                handleLocal(annotation, annotationNode, node);
                break;
        }
    }

    /**
     * 用来处理局部变量类
     * @param annotation
     * @param annotationNode
     * @param localNode
     */
    private void handleLocal(AnnotationValues<LogInfo> annotation, JavacNode annotationNode, JavacNode localNode) {
        JavacNode typeNode = JavacHandlerUtil.upToTypeNode(localNode);
        if(!createLogField(annotation, annotationNode, typeNode)){
            return;
        }
        String logRule = getLocalLogRule(localNode);


        //插入到当前局部变量的后面
        insertLogAfterLocal(localNode, logRule);

    }

    private void insertLogAfterLocal(JavacNode localNode, String logRule) {
        TreeMaker maker = localNode.getTreeMaker();
        JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) localNode.getNode();

        JCTree.JCExpression logParam = maker.Ident(jcVariableDecl.name);

        JCTree.JCExpression logMethod = JavacHandlerUtil.chainDotsString(localNode, "LOGGER.info");
        JCTree.JCMethodInvocation methodInvocation = maker.Apply(List.<JCTree.JCExpression>nil(),logMethod, List.of(maker.Literal(logRule), logParam));
        JCTree.JCExpressionStatement statement = maker.Exec(methodInvocation);
        JavacNode parentNode = localNode.getParent();
        //需要找到 Method
        if (parentNode.getKind() == NodeKind.METHOD){
            List<JCTree.JCStatement> insertAfter = ((JCTree.JCMethodDecl)parentNode.getNode()).body.stats;
            List<JCTree.JCStatement> insertBefore = ((JCTree.JCMethodDecl)parentNode.getNode()).body.stats.tail;
            while (true) {
                if (insertAfter.head == jcVariableDecl){
                    break;
                }
                insertAfter = insertBefore;
                insertBefore = insertBefore.tail;
            }
            List<JCTree.JCStatement> statementEntry = List.of(statement);
            statementEntry.tail = insertBefore;
            insertAfter.tail = statementEntry;
        }
        //或者if,for,lambda等语句块
        if (parentNode.getKind() == NodeKind.STATEMENT || parentNode.getNode() instanceof JCTree.JCBlock){
            List<JCTree.JCStatement> insertAfter = ((JCTree.JCBlock)parentNode.getNode()).stats;
            List<JCTree.JCStatement> insertBefore = ((JCTree.JCBlock)parentNode.getNode()).stats.tail;
            while (true) {
                if (insertAfter.head == jcVariableDecl){
                    break;
                }
                insertAfter = insertBefore;
                insertBefore = insertBefore.tail;
            }
            List<JCTree.JCStatement> statementEntry = List.of(statement);
            statementEntry.tail = insertBefore;
            insertAfter.tail = statementEntry;
        }

    }

    private String getLocalLogRule(JavacNode localNode) {
         StringBuilder logRule = new StringBuilder();
        JCTree.JCClassDecl type = (JCTree.JCClassDecl) JavacHandlerUtil.upToTypeNode(localNode).getNode();
        JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) JavacHandlerUtil.upToMethodNode(localNode).getNode();
        logRule.append(type.name);
        logRule.append(".");
        logRule.append(method.name);
        logRule.append(" localVariable ");
        logRule.append(((JCTree.JCVariableDecl)localNode.getNode()).name);
        logRule.append(":{}");
//        for (JavacNode paramNode : paramsNode){
//            logRule.append(((JCTree.JCVariableDecl)paramNode.getNode()).name);
//            logRule.append(": {} ,");
//        }
//        logRule.deleteCharAt(logRule.length()-1);
//        if (phase == "end"){
//            logRule.append(", result: {}");
//        }
        return logRule.toString();
    }

    private void handleType(AnnotationValues<LogInfo> annotation, JavacNode annotationNode, JavacNode typeNode) {
        if(!createLogField(annotation, annotationNode, typeNode)){
            return;
        }
        for (JavacNode child : typeNode.getChildren()){
            if (child.getKind() != NodeKind.METHOD){
                continue;
            }
            if (JavacHandlerUtil.hasAnnotation(child, LogInfo.class)){
             //跳过，因为filed会遍历的
                continue;
            }
            handleMethod(annotation, annotationNode, child);
        }
    }

    private void handleMethod(AnnotationValues<LogInfo> annotation, JavacNode annotationNode, JavacNode methodNode) {
        JCTree.JCMethodDecl methodDecl = ((JCTree.JCMethodDecl)methodNode.getNode());
        if (methodDecl.name.contentEquals("<init>")){
            return;
        }

        if (methodDecl.body == null || methodDecl.body.stats == null || methodDecl.body.stats.isEmpty()){
            return;
        }
        //提升至class的node
        JavacNode typeNode = JavacHandlerUtil.upToTypeNode(annotationNode);
        //创建LOGGER 类变量
        if (!createLogField(annotation, annotationNode, typeNode)){
            return;
        }

        //添加LOGGER.info("ClassName.MethodName invoke start param1name:{}, param2name:{}", param1, param2);
        insertLogInfoStart(methodNode);
        //添加LOGGER.info("ClassName.MethodName param1name:{}, param2name:{},result:{}", param1, param2, result);
        insertLogInfoEnd(methodNode);
    }

    /**
     * 由于return的并不是一个变量也有可能是一个表达式
     * @param methodNode
     */
    private void insertLogInfoEnd(JavacNode methodNode) {
        String logEndRule = getLogRule(methodNode, "end");
        TreeMaker maker = methodNode.getTreeMaker();
        Type returenType = ((JCTree.JCMethodDecl)methodNode.getNode()).restype.type;
        if (returenType == null || returenType instanceof Type.JCVoidType){
            return;
        }
        for (JavacNode node:methodNode.getChildren()) {
            if (node.getKind() == NodeKind.RERTUN){
                insertLogBeforReturn(node, returenType, logEndRule, maker);
            }
        }
    }

    private void insertLogBeforReturn(JavacNode node, Type returenType, String logEndRule, TreeMaker maker) {
        JCTree.JCReturn returnNode = (JCTree.JCReturn)node.getNode();
        ListBuffer<JCTree.JCExpression> logParams = new ListBuffer<>();
        JCTree.JCExpression logField = JavacHandlerUtil.chainDotsString(node, LOG_FIELD_NAME);
        logParams.add(logField);
        logParams.add(maker.Literal(logEndRule));
        JavacHandlerUtil.getParamsNode(node.getParent()).stream().forEach((paramNode ->{
            Name paramName = paramNode.toName((((JCTree.JCVariableDecl)paramNode.getNode()).name.toString()));
            logParams.add(maker.Ident(paramName));
        }));
        logParams.add(returnNode.expr);
        JCTree.JCExpression logMethod = JavacHandlerUtil.chainDotsString(node, "slothlog.handler.log.LogUtil.loggerInfo");
        JCTree.JCMethodInvocation methodInvocation = maker.Apply(List.nil(), logMethod, logParams.toList());
        JCTree.JCTypeCast typeCast = maker.TypeCast(returenType, methodInvocation);
        returnNode.expr = typeCast;
    }


    /**
     * 添加方法开头的日志信息
     * @param methodNode
     */
    private void insertLogInfoStart(JavacNode methodNode) {
        String logRule = getLogRule(methodNode, "start");
        TreeMaker maker = methodNode.getTreeMaker();
        ListBuffer<JCTree.JCExpression> logParams = new ListBuffer<>();
        logParams.add(maker.Literal(logRule));
        JavacHandlerUtil.getParamsNode(methodNode).stream().forEach((paramNode ->{
            Name paramName = paramNode.toName((((JCTree.JCVariableDecl)paramNode.getNode()).name.toString()));
            logParams.add(maker.Ident(paramName));
        }));
        JCTree.JCExpression logMethod = JavacHandlerUtil.chainDotsString(methodNode, "LOGGER.info");
        JCTree.JCMethodInvocation methodInvocation = maker.Apply(List.<JCTree.JCExpression>nil(),logMethod, logParams.toList());
        JCTree.JCExpressionStatement statement = maker.Exec(methodInvocation);
        ((JCTree.JCMethodDecl)methodNode.getNode()).body.stats = ((JCTree.JCMethodDecl)methodNode.getNode()).body.stats.prepend(statement);
        methodNode.getChildren().add(methodNode.getJavacAST().buildTree(statement, NodeKind.STATEMENT));
    }

    private String getLogRule(JavacNode methodNode, String phase) {
        //先获取param参数
        java.util.List<JavacNode> paramsNode = JavacHandlerUtil.getParamsNode(methodNode);
        StringBuilder logRule = new StringBuilder();
        JCTree.JCClassDecl type = (JCTree.JCClassDecl) JavacHandlerUtil.upToTypeNode(methodNode).getNode();
        JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) methodNode.getNode();
        logRule.append(type.name);
        logRule.append(".");
        logRule.append(method.name);
        logRule.append(" invoke " + phase + "  ");
        for (JavacNode paramNode : paramsNode){
            logRule.append(((JCTree.JCVariableDecl)paramNode.getNode()).name);
            logRule.append(": {} ,");
        }
        logRule.deleteCharAt(logRule.length()-1);
        if (phase == "end"){
            logRule.append(", result: {}");
        }
        return logRule.toString();
    }


    private boolean createLogField(AnnotationValues<LogInfo> annotation, JavacNode annotationNode, JavacNode typeNode) {
//        String logFieldName = annotation;
        JCTree.JCVariableDecl jcVariableDecl = JavacHandlerUtil.fieldExists(LOG_FIELD_NAME, typeNode);
        if (jcVariableDecl != null){
            //代码中自带的LOGGER
            if (jcVariableDecl.vartype.type != null && LOG_FULL_CLASS_NAME.equals(jcVariableDecl.vartype.type.toString())){
                jcVariableDecl.mods.flags = Flags.PRIVATE | Flags.FINAL | Flags.STATIC;
                return true;
            //自己生成的LOGGER
            } else if (jcVariableDecl.vartype.type == null && LOG_FULL_CLASS_NAME.equals(jcVariableDecl.vartype.toString())) {
                return true;
            } else {
                return false;
            }
        }
        //private static final {loggerType} LOGGER = {factoryMethod}({loggingType});
        //需要生成三个JCExpression
        //得到当前类class 如 Member.class
        JCTree.JCFieldAccess loggingType = JavacHandlerUtil.getSelfType(typeNode);
        JCTree.JCExpression loggerType = JavacHandlerUtil.chainDotsString(typeNode, LOG_FULL_CLASS_NAME);
        JCTree.JCExpression factoryMethod = JavacHandlerUtil.chainDotsString(typeNode, "org.slf4j.LoggerFactory.getLogger");
        TreeMaker maker = typeNode.getTreeMaker();
        //生成org.slf4j.LoggerFactory.getLogger(Member.class)
        JCTree.JCMethodInvocation factoryMethodCall = maker.Apply(List.<JCTree.JCExpression>nil(), factoryMethod,  List.<JCTree.JCExpression>of(loggingType));
        JCTree.JCVariableDecl fieldDecl = maker.VarDef(
                maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC ),
                typeNode.toName(LOG_FIELD_NAME), loggerType, factoryMethodCall);
        //插入AST抽象语法树
        JavacHandlerUtil.injectField(typeNode, fieldDecl);
        return true;
    }
}