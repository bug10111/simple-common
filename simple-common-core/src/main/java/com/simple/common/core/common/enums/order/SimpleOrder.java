package com.simple.common.core.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * 组件初始化顺序定义（实现 Spring Ordered，值越小越先初始化）
 * <p>取值即绝对顺序：Event(1) → Oauth(2) → Aviator(3) → Auth(4)；
 * 新增组件前请先确认与既有取值无冲突（避免无意覆盖既有组件的初始化先后关系）。
 * 常量名保持既有 PascalCase：AuthInit/AviatorInit/EventHandlerInit 等下游按名引用，改名是破坏性变更。</p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum SimpleOrder {
    Event(1), Oauth(2), Aviator(3), Auth(4);

    //初始化顺序（原始类型，杜绝null拆箱NPE；getOrder()实现Spring Ordered契约）
    private final int order;
}
