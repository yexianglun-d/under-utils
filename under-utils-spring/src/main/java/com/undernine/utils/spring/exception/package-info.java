/**
 * Spring 异常处理包
 * <p>
 * 提供业务异常类，用于统一处理业务逻辑中的异常情况。
 * </p>
 *
 * <h2>核心类</h2>
 * <ul>
 *     <li>{@link com.undernine.utils.spring.exception.BizException} - 业务异常类，继承自 RuntimeException</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 抛出业务异常
 * @Service
 * public class UserService {
 *     public User getById(Long id) {
 *         User user = userMapper.selectById(id);
 *         if (user == null) {
 *             throw new BizException("用户不存在");
 *         }
 *         return user;
 *     }
 *
 *     public void updateStatus(Long id, Integer status) {
 *         if (status < 0 || status > 1) {
 *             throw new BizException(ResultCode.PARAM_ERROR, "状态值无效");
 *         }
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * <h2>异常处理</h2>
 * <p>
     * 业务异常可以由显式注册的 {@link com.undernine.utils.spring.exception.GlobalExceptionHandler}
     * 转换为标准的 {@link com.undernine.utils.spring.result.Result} 格式返回。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
package com.undernine.utils.spring.exception;
