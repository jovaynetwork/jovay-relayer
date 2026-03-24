/*
 * Copyright 2026 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.l2.relayer.commons.exceptions;

import cn.hutool.core.util.StrUtil;

public class L2RelayerException extends RuntimeException {

    /**
     * Error code
     */
    private final String code;

    /**
     * Short message to
     */
    private final String msg;

    /**
     * {@code L2RelayerException} is the base exception for whole project.
     *
     * <p>
     * Other business exceptions need to be extended from {@code AntChainBridgeBaseException}.
     * </p>
     *
     * @param code    error code designed by business project
     * @param msg     message bound with code
     * @param message long message for your logger
     */
    private L2RelayerException(String code, String msg, String message) {
        super(message);
        this.code = code;
        this.msg = msg;
    }

    /**
     * {@code L2RelayerException} is the base exception for whole project.
     *
     * <p>
     * Other business exceptions need to be extended from {@code L2RelayerException}.
     * </p>
     *
     * @param code    error code designed by business project
     * @param msg     message bound with code
     * @param message long message for your logger
     * @param cause   business exception
     */
    private L2RelayerException(String code, String msg, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.msg = msg;
    }

    public L2RelayerException(L2RelayerErrorCodeEnum errorCode, String longMsg) {
        this(errorCode.getErrorCode(), errorCode.getShortMsg(), longMsg);
    }

    public L2RelayerException(L2RelayerErrorCodeEnum errorCode, String formatStr, Object... objects) {
        this(errorCode.getErrorCode(), errorCode.getShortMsg(), StrUtil.format(formatStr, objects));
    }

    public L2RelayerException(L2RelayerErrorCodeEnum errorCode, Throwable throwable, String formatStr, Object... objects) {
        this(errorCode.getErrorCode(), errorCode.getShortMsg(), StrUtil.format(formatStr, objects), throwable);
    }

    public L2RelayerException(L2RelayerErrorCodeEnum errorCode, String longMsg, Throwable throwable) {
        this(errorCode.getErrorCode(), errorCode.getShortMsg(), longMsg, throwable);
    }
}
