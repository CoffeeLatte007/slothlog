/**
 * lizhao
 * Copyright (c) 2010-2018 All Rights Reserved.
 */
package slothlog.handler.log;

import org.slf4j.Logger;

/**
 * <p></p>
 *
 * @author lizhao 563868273@qq.com
 * @version lizhao: LogUtil.java, v 0.1 2018-07-15 下午7:22  $$
 */
public class LogUtil {
    public static Object loggerInfo(Logger logger, String maker, Object... params) {

        logger.info(maker, params);
        if (params == null || params.length < 1){
            return null;
        }
        return params[params.length - 1];
    }
}