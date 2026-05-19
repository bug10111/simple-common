# java功能开发规范.md

**版本信息**
- 版本：v1.2（补充版）
- 适用范围：OAuth 管理系统 Web 前端所有代码（组件、API、DTO、工具函数等）
- 制定日期：2026-05-16
- 作者：qty

---

## 1. 技术栈规范

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | React | 18+ | 函数组件 + Hooks |
| 语言 | TypeScript | 5+ | 严格类型检查 |
| UI库 | Ant Design | 5+ | 统一组件库 |
| 样式 | TailwindCSS | 3+ | 辅助样式处理 |
| 路由 | React Router | 6+ | 路由管理 |
| 请求 | Axios | 1+ | HTTP客户端 |

---

## 2. 认证与请求规范

### 2.1 认证方式

| 接口类型 | 认证方式 | 请求头格式 |
|---------|---------|-----------|
| 登录 `/auth/login` | Basic Auth | `Authorization: Basic base64(clientId:clientSecret)` |
| 刷新 `/auth/refresh` | Basic Auth | `Authorization: Basic base64(clientId:clientSecret)` |
| 业务接口 | Bearer Token | `Authorization: Bearer {token}` |

### 2.2 Token 刷新机制

#### 2.2.1 刷新流程

1. 业务请求失败，错误码为 1000
2. 检查是否存在 refresh token 且未重试过
    - 若存在：调用 `/auth/refresh`（使用 Basic Auth）
        - 刷新成功：更新 accessToken 和 refreshToken，更新请求头，重试原始请求
        - 刷新失败：清除存储，跳转登录页
    - 若不存在：清除存储，直接跳转登录页

#### 2.2.2 字段映射规范

| 后端字段 | 前端字段 | 说明 |
|---------|---------|------|
| accessToken | accessToken | Access Token |
| refreshToken | refreshToken | Refresh Token |
| bearer | bearer | Token类型 |
| exp | exp | 过期时间戳 |
| scopes | scopes | 权限范围 |

#### 2.2.3 请求拦截器规则

```typescript
// 登录/刷新接口：Basic Auth
if (url === '/auth/login' || url === '/auth/refresh') {
  delete config.headers['Authorization']
  config.headers.Authorization = `Basic ${basicAuth}`
} else {
  // 业务接口：Bearer Token
  config.headers.Authorization = `Bearer ${token}`
}
```

### 2.3 请求封装

- 使用统一 axios 实例，复用拦截器配置
- 响应格式：`{ code: number, message: string, data: T }`
- 错误处理：全局响应拦截器统一处理

### 2.4 API 文件规范

- **文件名：** `{module}Api.ts`（如 `sysUserApi.ts`）
- **导出对象：** `{Module}Api`（如 `SysUserApi`）

---

## 3. 页面布局规范

### 3.1 整体布局结构

- **顶部 Header：** Logo/标题 + 操作按钮（修改密码/退出登录）
- **中间：** 左侧 Sidebar（菜单） + 右侧 Content Area
- **内容区：** 搜索区域 → 操作按钮区 → 数据表格

### 3.2 搜索区域样式

| 属性 | 值 |
|------|-----|
| 背景色 | `#f5f5f5` |
| 圆角 | `8px` |
| 内边距 | `16px` |
| 外边距（底部） | `16px` |
| 输入框宽度（普通） | `200px` |
| 输入框宽度（下拉选择） | `120px` |

### 3.3 操作按钮区布局

- 左侧：全选复选框 + 批量操作按钮
- 右侧：新增按钮
- 使用 `display: flex; justify-content: space-between`

---

## 4. 表格规范

### 4.1 表格样式

| 属性 | 值 |
|------|-----|
| 表头对齐 | 居中 |
| 内容对齐 | 居中 |
| 操作列宽度 | `150px` |

### 4.2 行操作按钮规范

- **常用操作：** 直接显示按钮
- **次要操作：** 放入「更多」下拉菜单
- **危险操作：** 使用 `danger` 属性标记

### 4.3 表格列设计规范（新增）

- **备注列：** 必须使用 `ellipsis: true` 实现自适应宽度，鼠标悬浮时通过 Tooltip 查看完整内容
- **关联数据列：** 一律显示名称而非 ID（例如显示"张三"而非用户ID）

```tsx
<Column title="状态" render={(status) => <Tooltip title={getStatusLabel(status)}>{getStatusLabel(status)}</Tooltip>} />
```

---

## 5. 弹窗规范

### 5.1 弹窗尺寸

| 类型 | 宽度 | 说明 |
|------|------|------|
| 表单弹窗 | `520px` | 创建/编辑表单 |
| 确认弹窗 | `420px` | 删除/重置确认 |

### 5.2 弹窗属性

```tsx
<Modal
  open={showModal}
  onCancel={() => setShowModal(false)}
  title={弹窗标题}
  width={520}
  centered
  footer={[
    <Button key="cancel" onClick={() => setShowModal(false)}>取消</Button>,
    <Button key="confirm" type="primary">确认</Button>
  ]}
>
  {/* 表单内容 */}
</Modal>
```

### 5.3 表单布局规范

#### 5.3.1 表单容器

```tsx
<div style={{ width: '85%', margin: '0 auto' }}>
  {/* 表单项 */}
</div>
```

#### 5.3.2 表单项配置

| 属性 | 值 | 说明 |
|------|-----|------|
| layout | `horizontal` | 水平布局 |
| size | `middle` | 中等尺寸 |
| labelCol | `{ span: 5 }` | 标签占5列 |
| wrapperCol | `{ span: 19 }` | 输入框占19列 |
| marginBottom | `16px` | 字段间距（最后一项设为0） |

#### 5.3.3 输入框样式

| 选项 | 值 |
|------|-----|
| 高度 | `36px` |
| 字体大小 | `14px` |
| 内边距 | `0 12px` |

### 5.4 默认值设置规范

> **重要：** `defaultValue` 仅设置组件初始显示值，不会自动将值设置到表单字段中。

**正确做法：**

```typescript
const openCreateModal = () => {
  createForm.resetFields()
  createForm.setFieldsValue({ permissionScope: 'ALL' })
  setShowCreateModal(true)
}
```

> **新增补充：** 除特殊业务明确要求外，禁止自动为业务字段设置默认值。一般场景下应保持表单为空，由用户主动填写。

### 5.5 必填项处理规范

#### 5.5.1 必填项标记

```tsx
<Form.Item label="名称" name="name" rules={[{ required: true, message: '请输入名称' }]}>
```

#### 5.5.2 ID类型必填项处理

- **关联其他实体的ID：** 必须提供下拉选择，从对应管理接口获取列表
- **系统自动生成的ID：** 隐藏字段，编辑时传递

### 5.6 弹窗按钮规范

- 左侧：取消按钮（默认样式）
- 右侧：确认按钮（`type="primary"`）
- 危险操作：确认按钮添加 `danger` 属性

### 5.7 表单字段动态监听与默认值管理（新增）

- **动态字段：** 必须使用 `Form.useWatch()` 监听字段变化，禁止使用 `form.watch()`
- **示例：**

```typescript
const statusValue = Form.useWatch('status', form);
```

### 5.8 作用域与关联字段选择规范（新增）

- **作用域字段：** 使用 Select 的 `tags` 模式，支持用户选择或输入自定义内容
- **关联字段（如项目、部门）：** 必须使用下拉选择，前端展示名称，提交时传递对应的 ID

```tsx
<Select mode="tags" placeholder="选择或输入作用域">
  {/* 选项 */}
</Select>

<Select placeholder="选择项目">
  {projects.map(p => <Option key={p.id} value={p.id}>{p.name}</Option>)}
</Select>
```

---

## 6. 按钮规范

| 类型 | 样式 | 场景 |
|------|------|------|
| 主按钮 | `type="primary"` | 确认提交、搜索、新增 |
| 次按钮 | 默认 | 取消、重置 |
| 危险按钮 | `danger` | 删除、重置密码 |
| 链接按钮 | `type="link"` | 表格行内操作 |

### 6.1 防重复点击规范（新增）

- 所有提交按钮（确认创建、确认编辑、导出等）必须使用 `usePreventDoubleClick` Hook
- Hook 自动管理 loading 状态，在请求期间禁用按钮

```typescript
const { onClick: handleCreate, loading: createLoading } = usePreventDoubleClick(async () => {
  const values = await createForm.validateFields();
  await SysProjectApi.create(values);
  ToastUtil.success('创建成功');
  setShowCreateModal(false);
  loadData();
});

<Button type="primary" loading={createLoading} onClick={handleCreate}>确认创建</Button>
```

---

## 7. 异常处理规范

### 7.1 全局异常提示

- 使用 `ToastUtil` 统一显示
- 位置：右上角固定
- z-index：9999
- 文案：居中显示

### 7.2 错误码处理优先级

业务错误码（1000/1001/1002/1003/1004） > HTTP状态码（500） > 其他错误

### 7.3 错误码处理规则

| 错误码 | HTTP状态 | 处理逻辑 |
|--------|---------|---------|
| 1000 | 任意 | 检查 refresh token，有则刷新，无则跳转登录 |
| 1001 | 任意 | 清除存储，跳转登录页 |
| 1002 | 任意 | 显示警告："权限不足" |
| 1003 | 任意 | 显示错误："登录失败次数过多" |
| 1004 | 任意 | 显示错误："IP登录失败次数过多" |
| 无业务码 | 500 | 显示后端返回的 message |
| 其他 | 任意 | 显示后端返回的 message |

---

## 8. 状态管理规范

### 8.1 本地状态

- 使用 `useState` 管理组件内部状态
- 复杂状态逻辑使用 `useCallback` 优化

### 8.2 存储管理

| 数据 | 存储方式 |
|------|---------|
| Token | localStorage（通过 `storageUtil`） |
| Refresh Token | localStorage（通过 `storageUtil`） |
| 用户信息 | 无需存储（后端从 token 解析） |

---

## 9. UI 一致性规范

### 9.1 组件风格一致性（重要）

> 每个界面的相同组件，在相同或类似用法时，视觉风格（配色、圆角、间距比例、交互反馈）需保持一致。
> **不要求像素级完全一致**——不同表单字段数量、内容长度不同，尺寸和布局会有合理差异，只需整体视觉感受统一即可。

#### 9.1.1 通用组件规范

- **按钮：** 统一使用 Ant Design Button 组件，主按钮 `type="primary"`，取消按钮默认样式，危险操作 `danger` 属性
- **输入框：** 统一高度 `36px`，字体大小 `14px`，内边距 `0 12px`
- **下拉选择：** 统一高度 `36px`，字体大小 `14px`
- **表格：** 统一使用带边框样式，表头居中，内容居中，操作列宽度 `150px`

#### 9.1.2 布局风格一致性

- **搜索区域：** 统一背景色 `#f5f5f5`，圆角 `8px`，内边距 `16px`，底部外边距 `16px`
- **操作按钮区：** 统一 `display: flex; justify-content: space-between`
- **弹窗：** 表单弹窗参考宽度 `520px`，确认弹窗参考宽度 `420px`，标题居中显示（弹窗宽度可根据表单内容量灵活调整）

#### 9.1.3 表单风格一致性

> 表单整体视觉风格保持统一（布局方向、标签对齐、间距节奏），具体数值可根据表单字段数量和内容适当调整。

| 属性 | 参考值 | 说明 |
|------|--------|------|
| 布局 | `horizontal` | 水平布局 |
| 尺寸 | `middle` | 中等尺寸 |
| 标签列 | `{ span: 5 }` | 参考，字段多时可微调 |
| 输入框列 | `{ span: 19 }` | 参考，与标签列配合 |
| 字段间距 | `16px` | 参考，最后一项 `0` |

#### 9.1.4 交互一致性

- **提示信息：** `ToastUtil`，右上角固定
- **加载状态：** 表格 `loading` 属性或按钮 `loading` 属性
- **确认弹窗：** 危险按钮样式，文案居中

#### 9.1.5 命名一致性

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| 组件文件 | `{功能}ManagementComponent.tsx` | `SysUserManagementComponent.tsx` |
| API文件 | `{module}Api.ts` | `sysUserApi.ts` |
| DTO文件 | `{功能}{类型}Dto.ts` | `CreateSysUserRequestDto.ts` |

### 9.2 代码质量规范（新增）

- **移除未使用的导入：** 提交前必须清理所有未使用的 import 声明
- **使用 `Form.useWatch()` 而非 `form.watch()`**

---

## 10. 数据展示规范

### 10.1 关联数据展示规则

> **重要：** 所有 Controller 接口返回的列表数据，凡是带有 ID 的，首选连接查询获取其名称，然后返回。

#### 10.1.1 关联实体ID处理

- **关联其他实体的ID（如项目ID、部门ID、用户ID）：** 后端通过 JOIN 查询获取名称，前端直接显示名称字段
- **字典类型字段（如状态、类型）：** 前端在界面初始化时加载对应类型的字典数据，根据 code 显示中文

#### 10.1.2 字典数据加载规范

- 在组件初始化时（`useEffect`）加载所需的字典类型数据
- 将字典数据存储在组件状态中（如 `dictDataMap`）
- 使用 `useCallback` 封装字典值转中文的函数
- **禁止直接显示字典 code 值，必须转换为中文显示**

#### 10.1.3 示例代码

```typescript
// 加载字典数据
const [statusDict, setStatusDict] = useState<{ code: string; label: string }[]>([])

useEffect(() => {
  const loadDict = async () => {
    const result = await SysDictDataApi.list({ dictTypeCode: 'status' })
    setStatusDict(result.records)
  }
  loadDict()
}, [])

// 字典值转中文
const getStatusLabel = useCallback((code: string) => {
  const item = statusDict.find(d => d.code === code)
  return item?.label || code
}, [statusDict])

// 使用
<Column title="状态" render={(status) => <Tooltip title={getStatusLabel(status)}>{getStatusLabel(status)}</Tooltip>} />
```

---

## 11. 安全规范

### 11.1 密码处理

- **修改密码：** 后端从 token 获取用户 ID，前端不传递
- **重置密码：** 管理员操作，使用系统默认密码
- **密码存储：** 后端 BCrypt 加密

### 11.2 敏感信息

- 禁止在前端存储敏感信息
- 日志中不记录密码等敏感数据
- **前后端交互补充：** 后端不应返回敏感信息（如密钥），若需提供文件下载，应单独提供下载接口，由前端触发下载

### 11.3 Token 安全

- **Access Token：** 短期有效，用于业务请求
- **Refresh Token：** 长期有效，仅用于刷新 Access Token
- 刷新失败时立即清除所有存储并跳转登录

---

## 12. 构建规范

### 12.1 构建命令

```bash
npm run build  # TypeScript编译 + Vite构建
```

### 12.2 检查流程

1. TypeScript 类型检查
2. ESLint 代码检查
3. 单元测试（可选）
4. 构建验证

---

## 13. 命名规范

### 13.1 标识符后缀

| 类型 | 后缀 | 示例 |
|------|------|------|
| 组件 | `Component` | `UserProfileComponent` |
| API封装 | `Api` | `UserApi` |
| 数据传输对象 | `Dto` | `LoginDto` |
| 自定义Hook | `Hook` | `useWindowSizeHook` |
| 工具函数 | `Util` | `DateUtil` |

### 13.2 权限标识规则

- **格式：** 类级别 `@RequestMapping` 路径:方法路径
- **示例：** `sys:department:tree`

---

## 14. 组件开发检查清单

### 14.1 通用检查

- [ ] 调用了 simple-common 对应技能并查阅文档
- [ ] 所有引用的类、方法已验证存在
- [ ] 共享变量已使用同步机制
- [ ] 已执行 `npm run build` 并成功
- [ ] 代码逻辑已验证闭环
- [ ] `@author` 已设置为 `qty`

### 14.2 表单弹窗检查

- [ ] 弹窗标题已居中
- [ ] 表单布局为 `horizontal`
- [ ] 输入框高度为 `36px`
- [ ] 字段间距为 `16px`
- [ ] 表单容器宽度为 `85%`，居中显示
- [ ] 所有必填项已添加验证规则
- [ ] 枚举类型字段已设置默认值（仅必要情况）
- [ ] ID类型必填项已实现下拉选择
- [ ] 编辑弹窗已正确回显数据
- [ ] 未自动设置非必要默认值
- [ ] 动态字段使用 `Form.useWatch()`

### 14.3 接口对接检查

- [ ] 查看后端 DTO 识别所有必填字段
- [ ] ID类型字段已实现下拉选择
- [ ] 枚举类型字段已设置默认值（仅必要情况）
- [ ] 表单验证规则完整

### 14.4 数据展示检查

- [ ] 关联实体ID已通过后端 JOIN 查询获取名称
- [ ] 字典类型字段已在初始化时加载字典数据
- [ ] 字典 code 已转换为中文显示
- [ ] 禁止直接显示 ID 或 code 值
- [ ] 备注列添加 `ellipsis: true`
- [ ] 关联列显示名称而非 ID

### 14.5 防重复点击检查（新增）

- [ ] 所有提交按钮已使用 `usePreventDoubleClick` Hook
- [ ] 按钮的 loading 状态已正确绑定
- [ ] 异步请求期间按钮禁用，请求完成后恢复

---

## 15. 质量优先级

1. 线程安全 > 高性能 > 低内存
2. 性能与线程安全冲突时，优先保证线程安全
3. 任何情况下，不得牺牲线程安全换取性能或内存优化

---

## 16. 前后端数据交互与全局配置规范（新增）

### 16.1 时间格式化

- **后端：** 返回原始时间格式（如 Date 或时间戳）
- **前端：** 通过全局配置（如 dayjs 全局拦截器）统一格式化显示，不在每个组件中单独处理
- **示例：** 在 axios 响应拦截器或全局渲染函数中将时间字段统一转换为 `YYYY-MM-DD HH:mm:ss`

### 16.2 敏感信息

- **后端：** 禁止在响应 DTO 中返回敏感信息（如密钥、明文密码）
- **前端：** 若需下载敏感数据（如密钥文件），后端提供专用下载接口，前端通过 `<a>` 或 `window.open` 触发下载

### 16.3 外键处理（重申）

- **后端：** JOIN 查询获取名称
- **前端：** 展示名称，提交时传递 ID

---

## 附录：常用代码模板（示例）

### A.1 完整创建弹窗示例（遵循防重复点击、无默认值）

```tsx
const [showCreateModal, setShowCreateModal] = useState(false)
const [createForm] = Form.useForm()

const { onClick: handleCreate, loading: createLoading } = usePreventDoubleClick(async () => {
  const values = await createForm.validateFields()
  await Api.create(values)
  ToastUtil.success('创建成功')
  setShowCreateModal(false)
  loadData()
})

<Modal
  open={showCreateModal}
  onCancel={() => setShowCreateModal(false)}
  title="新增数据"
  width={520}
  centered
  footer={[
    <Button key="cancel" onClick={() => setShowCreateModal(false)}>取消</Button>,
    <Button key="confirm" type="primary" loading={createLoading} onClick={handleCreate}>确认创建</Button>
  ]}
>
  {/* 表单内容 */}
</Modal>
```

### A.2 下拉选择实现示例

```tsx
const [projects, setProjects] = useState<{ id: string; name: string }[]>([])

const loadProjects = useCallback(async () => {
  const result = await SysProjectApi.list({ current: 1, size: 100 })
  setProjects(result.records.map(p => ({ id: p.id, name: p.name })))
}, [])

<Select placeholder="选择项目">
  {projects.map(project => <Option key={project.id} value={project.id}>{project.name}</Option>)}
</Select>
```

### A.3 字典数据加载与使用示例

```tsx
const [statusDict, setStatusDict] = useState<{ code: string; label: string }[]>([])

useEffect(() => {
  const loadDict = async () => {
    const result = await SysDictDataApi.list({ dictTypeCode: 'status' })
    setStatusDict(result.records)
  }
  loadDict()
}, [])

const getStatusLabel = useCallback((code: string) => {
  const item = statusDict.find(d => d.code === code)
  return item?.label || code
}, [statusDict])

<Column title="状态" render={(status) => <Tooltip title={getStatusLabel(status)}>{getStatusLabel(status)}</Tooltip>} />
```
