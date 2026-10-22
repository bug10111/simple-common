package com.simple.common.auth.client.controller;

import com.simple.common.auth.client.common.annotation.HasAuthority;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.IdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 权限导出控制器。
 * <p>
 * 自动扫描当前服务中所有 Controller，提取 {@link Tag}、{@link RequestMapping} 和 {@link HasAuthority} 注解，
 * 生成完整的 sys_menu 三级菜单 INSERT SQL（一级菜单 → 二级菜单 → 功能权限）。
 * client_id 从配置项 {@code simple.auth.project-code} 获取。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Tag(name = "权限导出")
@RequestMapping("auth/permission-export")
@RestController
public class PermissionExportController {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @GetMapping("sql")
    @HasAuthority("system:permission-export:sql")
    @Operation(summary = "导出当前服务完整菜单 SQL（含一级/二级菜单 + 功能权限）",
            description = "扫描所有 Controller 中的 @Tag、@RequestMapping、@HasAuthority 注解，" +
                    "自动生成三级菜单结构的 INSERT SQL 语句。" +
                    "client_id 来自配置项 simple.auth.project-code。" +
                    "生成的 SQL 包含一级菜单（@Tag）、二级菜单（@RequestMapping）、功能权限（@HasAuthority）三层结构。" +
                    "component 和 icon 字段设为 NULL，需手动补充。" +
                    "注意：ID 每次导出均新生成（雪花ID），脚本为一次性初始化脚本，重复执行将产生重复数据。")
    public R<Map<String, Object>> exportSql() {
        String clientId = authProperties.getProjectCode();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));

        // 按 Controller 分组收集所有权限信息
        Map<Class<?>, ControllerMenuInfo> controllerMap = scanControllerMenus();

        // 按 Tag 分组（用于生成一级菜单）
        Map<String, List<ControllerMenuInfo>> tagGroupMap = controllerMap.values().stream()
                .collect(Collectors.groupingBy(ControllerMenuInfo::getTagName,
                        LinkedHashMap::new, Collectors.toList()));

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("-- ========================================\n");
        sqlBuilder.append("-- 完整菜单权限初始化 SQL（自动生成，一次性脚本：重复执行将产生重复数据）\n");
        sqlBuilder.append("-- 客户端ID: ").append(escapeSql(clientId)).append("\n");
        sqlBuilder.append("-- 生成时间: ").append(now).append("\n");
        sqlBuilder.append("-- 一级菜单数: ").append(tagGroupMap.size()).append("\n");
        sqlBuilder.append("-- 二级菜单数: ").append(controllerMap.size()).append("\n");
        sqlBuilder.append("-- ========================================\n\n");
        sqlBuilder.append("-- 注意：component 和 icon 字段为 NULL，请根据实际情况手动补充\n\n");

        int tagSort = 0;
        int globalSort = 0;

        // level1Code 去重集合：中文 Tag 名归一化后可能冲突（重复 code 破坏 sys_menu 唯一性）
        Map<String, Integer> level1CodeUsed = new java.util.HashMap<>();

        // 用于收集返回的结构化数据
        List<Map<String, Object>> menuTree = new ArrayList<>();

        for (Map.Entry<String, List<ControllerMenuInfo>> tagEntry : tagGroupMap.entrySet()) {
            tagSort++;
            String tagName = tagEntry.getKey();
            List<ControllerMenuInfo> controllers = tagEntry.getValue();

            // 生成一级菜单 ID
            String level1Id = IdUtils.getSnowflakeNextIdStr();
            // 中文名归一化为纯下划线时回退 hash 形态，并按占用序号去重，避免无意义/重复 code
            String level1Code = normalizeMenuCode(tagName, level1CodeUsed);

            String level1Sql = String.format(
                    "INSERT INTO \"public\".\"sys_menu\" VALUES ('%s', '%s', '%s', '%s', '0', %d, NULL, NULL, NULL, 0, NULL, 'M', NULL, NULL, NULL, NULL, '', '%s', '', '%s');",
                    level1Id,
                    escapeSql(clientId),
                    escapeSql(tagName),
                    escapeSql(level1Code),
                    tagSort,
                    now,
                    now
            );
            sqlBuilder.append(level1Sql).append("\n");

            // 一级菜单结构化数据
            Map<String, Object> level1Node = new LinkedHashMap<>();
            level1Node.put("menuName", tagName);
            level1Node.put("menuType", "M");
            level1Node.put("sort", tagSort);
            // 类型化局部引用：避免 Object 强转（编译器无法校验 children 元素类型）
            List<Map<String, Object>> level1Children = new ArrayList<>();
            level1Node.put("children", level1Children);

            int controllerSort = 0;
            for (ControllerMenuInfo controller : controllers) {
                controllerSort++;

                // 生成二级菜单 ID
                String level2Id = IdUtils.getSnowflakeNextIdStr();
                String level2Code = controller.getModuleName();
                String level2Path = controller.getPath();

                String level2Sql = String.format(
                        "INSERT INTO \"public\".\"sys_menu\" VALUES ('%s', '%s', '%s', '%s', '%s', %d, '%s', NULL, NULL, 0, NULL, 'M', '%s', NULL, NULL, NULL, '', '%s', '', '%s');",
                        level2Id,
                        escapeSql(clientId),
                        escapeSql(controller.getMenuDisplayName()),
                        escapeSql(level2Code),
                        level1Id,
                        controllerSort,
                        escapeSql(level2Path),
                        // 二级菜单的 permission 取该 Controller 下第一个 @HasAuthority 的值（用于前端权限判断）
                        escapeSql(controller.getFirstPermission() != null ? controller.getFirstPermission() : ""),
                        now,
                        now
                );
                sqlBuilder.append(level2Sql).append("\n");

                // 二级菜单结构化数据
                Map<String, Object> level2Node = new LinkedHashMap<>();
                level2Node.put("menuName", controller.getMenuDisplayName());
                level2Node.put("menuType", "M");
                level2Node.put("path", level2Path);
                level2Node.put("sort", controllerSort);
                level2Node.put("permission", controller.getFirstPermission());
                List<Map<String, Object>> funcList = new ArrayList<>();
                level2Node.put("children", funcList);

                int funcSort = 0;
                for (PermissionInfo perm : controller.getPermissions()) {
                    funcSort++;
                    globalSort++;
                    String funcId = IdUtils.getSnowflakeNextIdStr();
                    String funcCode = perm.getPermission().replace(":", "_");

                    String funcSql = String.format(
                            "INSERT INTO \"public\".\"sys_menu\" VALUES ('%s', '%s', '%s', '%s', '%s', %d, NULL, NULL, NULL, 0, NULL, 'C', '%s', NULL, NULL, NULL, '', '%s', '', '%s');",
                            funcId,
                            escapeSql(clientId),
                            escapeSql(perm.getMenuName()),
                            escapeSql(funcCode),
                            level2Id,
                            funcSort,
                            escapeSql(perm.getPermission()),
                            now,
                            now
                    );
                    sqlBuilder.append(funcSql).append("\n");

                    // 功能权限结构化数据
                    Map<String, Object> funcNode = new LinkedHashMap<>();
                    funcNode.put("menuName", perm.getMenuName());
                    funcNode.put("menuType", "C");
                    funcNode.put("permission", perm.getPermission());
                    funcNode.put("sort", funcSort);
                    funcNode.put("sourceMethod", perm.getSourceMethod());
                    funcList.add(funcNode);
                }

                level1Children.add(level2Node);
            }

            menuTree.add(level1Node);
        }

        // 构建返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientId", clientId);
        result.put("level1Count", tagGroupMap.size());
        result.put("level2Count", controllerMap.size());
        result.put("permissionCount", globalSort);
        result.put("sql", sqlBuilder.toString());
        result.put("menuTree", menuTree);

        return R.ok(result);
    }

    /**
     * 扫描所有 Controller，按类分组收集菜单和权限信息。
     * <p>
     * 遍历前对 handler 按「类名 + 路径」排序：底層 Map 为 HashMap，迭代序随运行漂移，
     * 不排序则生成的 SQL/tagSort/controllerSort 每次导出都不一致（diff 噪音且不可复现）。
     * 无 @HasAuthority 的类不注册菜单分组（避免空菜单与框架控制器混入）。
     * </p>
     */
    private Map<Class<?>, ControllerMenuInfo> scanControllerMenus() {
        Map<Class<?>, ControllerMenuInfo> controllerMap = new java.util.TreeMap<>(java.util.Comparator.comparing(Class::getName));

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        // 预排序保证确定性（TreeMap 仅保证分组键有序，注册顺序仍依赖遍历序）
        List<Map.Entry<RequestMappingInfo, HandlerMethod>> entries = new ArrayList<>(handlerMethods.entrySet());
        entries.sort(java.util.Comparator
                .comparing((Map.Entry<RequestMappingInfo, HandlerMethod> e) -> e.getValue().getBeanType().getName())
                .thenComparing(e -> String.valueOf(e.getKey().toString())));

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : entries) {
            HandlerMethod handlerMethod = entry.getValue();
            Class<?> controllerClass = handlerMethod.getBeanType();

            // 先判权限注解：无任何 @HasAuthority 的类不生成菜单分组（跳过 BasicErrorController 等框架控制器）
            HasAuthority hasAuthority = handlerMethod.getMethodAnnotation(HasAuthority.class);
            if (hasAuthority == null) {
                continue;
            }

            // 获取或创建 Controller 菜单信息
            ControllerMenuInfo controllerInfo = controllerMap.computeIfAbsent(controllerClass, clazz -> {
                ControllerMenuInfo info = new ControllerMenuInfo();

                // 获取 @Tag 注解
                Tag tag = clazz.getAnnotation(Tag.class);
                info.setTagName(tag != null && tag.name() != null && !tag.name().isEmpty()
                        ? tag.name() : clazz.getSimpleName().replace("Controller", ""));

                // 获取 @RequestMapping 路径（取第一个路径）
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                if (requestMapping != null && requestMapping.value().length > 0) {
                    String path = requestMapping.value()[0];
                    info.setPath(path);
                    // 从路径推断模块名：取最后一段
                    String[] parts = path.split("/");
                    String moduleName = parts[parts.length - 1].replace("-", "_");
                    // 根路径/空段会产生空 code：回退类简名（空 permission_code 会破坏唯一性约束）
                    if (moduleName == null || moduleName.isEmpty()) {
                        moduleName = clazz.getSimpleName().replace("Controller", "").toLowerCase();
                    }
                    info.setModuleName(moduleName);
                    // 菜单显示名：从路径最后一段的驼峰转中文（或直接用路径）
                    info.setMenuDisplayName(path);
                } else {
                    info.setPath("");
                    info.setModuleName(clazz.getSimpleName().replace("Controller", "").toLowerCase());
                    info.setMenuDisplayName(clazz.getSimpleName().replace("Controller", ""));
                }

                return info;
            });

            // 获取 @Operation 注解的 summary 作为菜单名称
            Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
            String menuName;
            if (operation != null && operation.summary() != null && !operation.summary().isEmpty()) {
                menuName = operation.summary();
            } else {
                menuName = handlerMethod.getMethod().getName();
            }

            // @HasAuthority 的 value 是数组，遍历所有值
            String[] authorities = hasAuthority.value();
            for (String authority : authorities) {
                if (authority == null || authority.isEmpty()) {
                    continue;
                }
                PermissionInfo perm = new PermissionInfo();
                perm.setPermission(authority);
                perm.setMenuName(menuName);
                perm.setSourceMethod(handlerMethod.getMethod().getName());
                controllerInfo.addPermission(perm);
            }
        }

        return controllerMap;
    }

    /**
     * 菜单 code 归一化：非 ASCII（中文 Tag）归一后无有效字符时回退 hash 形态；
     * 重复 code 按占用序号追加后缀，保证 sys_menu.permission_code 唯一。
     */
    private String normalizeMenuCode(String tagName, Map<String, Integer> used) {
        String base = tagName == null ? "" : tagName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        // 全部为下划线/空（中文标签的常见结果）时回退 hash 形态，避免产出空/全下划线的无意义 code
        if (base.isEmpty() || base.chars().allMatch(c -> c == '_')) {
            base = "menu_" + Integer.toHexString(tagName == null ? 0 : tagName.hashCode());
        }
        int count = used.merge(base, 1, Integer::sum);
        return count == 1 ? base : base + "_" + count;
    }

    /**
     * SQL 字符串转义：单引号替换为两个单引号
     */
    private static String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    /**
     * Controller 菜单信息
     */
    private static class ControllerMenuInfo {
        /** @Tag 名称，用作一级菜单 */
        private String tagName;
        /** @RequestMapping 路径，用作二级菜单路径 */
        private String path;
        /** 模块名（路径最后一段），用作 permission_code */
        private String moduleName;
        /** 菜单显示名 */
        private String menuDisplayName;
        /** 该 Controller 下的所有权限 */
        private List<PermissionInfo> permissions = new ArrayList<>();

        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }
        public String getMenuDisplayName() { return menuDisplayName; }
        public void setMenuDisplayName(String menuDisplayName) { this.menuDisplayName = menuDisplayName; }
        public List<PermissionInfo> getPermissions() { return permissions; }
        public void addPermission(PermissionInfo perm) { this.permissions.add(perm); }

        /** 获取第一个权限标识（用于二级菜单的 permission 字段） */
        public String getFirstPermission() {
            return permissions.isEmpty() ? null : permissions.get(0).getPermission();
        }
    }

    /**
     * 功能权限信息
     */
    private static class PermissionInfo {
        private String permission;
        private String menuName;
        private String sourceMethod;

        public String getPermission() { return permission; }
        public void setPermission(String permission) { this.permission = permission; }
        public String getMenuName() { return menuName; }
        public void setMenuName(String menuName) { this.menuName = menuName; }
        public String getSourceMethod() { return sourceMethod; }
        public void setSourceMethod(String sourceMethod) { this.sourceMethod = sourceMethod; }
    }
}
