/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.javac.node;

/**
 * <p></p>
 *  用于定义JavacNode类型的枚举
 * @author lizhao 563868273@qq.com
 * @version lizhao: NodeKind.java, v 0.1 2018-07-14 下午3:30  Exp $$
 */
public enum NodeKind {
    /**
     * 根节点
     */
    COMPILATION_UNIT,
    /**
     * 类或者接口
     */
    TYPE,
    /**
     * 类的变量，常量
     */
    FIELD,
    /**
     * 方法
     */
    METHOD,
    /**
     * 注解
     */
    ANNOTATION,
    /**
     * 参数
     */
    ARGUMENT,
    /**
     * 局部变量
     */
    LOCAL,
    /**
     * 方法Body里面的语句
     */
    STATEMENT,
    /**
     * 类里面的静态变量块
     */
    INITIALIZER,
    /**
     * 方法的返回值
     */
    RERTUN;
}