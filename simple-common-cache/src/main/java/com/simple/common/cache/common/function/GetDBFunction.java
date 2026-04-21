package com.simple.common.cache.common.function;

/**
 * 数据库查询函数式接口。
 * <p>
 * 用于封装从数据库获取数据的逻辑,通常与缓存框架配合使用,实现“缓存-数据库”双层数据访问模式。
 * 当缓存未命中时,通过此接口从数据库加载数据并回写到缓存中。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>缓存穿透防护：数据库查询结果为null时,缓存空值占位对象</li>
 *   <li>懒加载：仅在缓存未命中时才查询数据库,减少不必要的IO操作</li>
 *   <li>数据同步：保证缓存与数据库的数据一致性</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 定义数据库查询函数
 * GetDBFunction<User, String> dbQuery = (userId) -> {
 *     // 从数据库查询用户信息
 *     return userMapper.selectById(userId);
 * };
 * 
 * // 配合缓存使用
 * User user = cacheService.getOrLoad("user:" + userId, userId, dbQuery);
 * }</pre>
 *
 * @param <T> 返回值类型,通常为实体类或DTO
 * @param <R> 请求参数类型,如ID、查询条件等
 * @author qty
 */
@FunctionalInterface
public interface GetDBFunction<T, R> {

    /**
     * 从数据库获取数据
     * <p>
     * 根据请求参数从数据库查询数据。如果数据不存在,应返回null,
     * 由上层缓存框架决定是缓存空值还是抛出异常。
     * </p>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>此方法应该只负责查询,不应包含业务逻辑</li>
     *   <li>建议在此方法中添加日志,便于排查缓存未命中的原因</li>
     *   <li>对于耗时较长的查询,应考虑添加超时控制</li>
     * </ul>
     *
     * @param request 请求参数,如主键ID、查询条件等
     * @return 查询结果,数据不存在时返回null
     */
    T get(R request);
}