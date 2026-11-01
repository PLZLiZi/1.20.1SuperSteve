# SuperSteve JEdit 热补丁系统 & 特效开发笔记

## 一、方法级热补丁（MethodPatcher）

### 核心原理

```
反编译原文 + 编辑后源码 → diff → 提取被修改方法 → 子类骨架热编译
→ obfB 重映射 → 方法体移植回原类 → Agt.retransform 一次性应用
```

- 子类骨架：`public abstract class 目标$$SSPatch extends 目标类`
- 编译器视野中的目标类成员权限被 HotCplr 用 ASM 放宽为 public（widenAccess）
- Agt.retransform 是原子操作：字节码非法时 JVM 拒绝，原类保留

### 关键处理点（按踩坑顺序）

#### 1. CFR 伪影清理
CFR 反编译被 Mixin/注入器修改过的类会产生非法 Java 伪影，提取方法时必须清理：
- `void paramName;` → 删除（CFR 把重命名参数输出为 void 声明）
- `/* WARNING - void declaration */` → 删除
- `/* Unavailable Anonymous Inner Class!! */` → 无法自动修复，编译会报错

#### 2. 注解剥离
方法头上的**所有**注解必须剥离（不只是 @Override）：
- `@MixinMerged(mixin="...", sessionId="...")` → sessionId() 无法解析
- `@Unique`、`@Nullable`、`@Override` 等在子类中无用或冲突
- 两步：先删整行注解，再删行内注解

#### 3. 方法名提取不能被注解干扰
`addMember` 分类和名字提取前必须先剥离注解行。
`@MixinMerged(mixin="...", sessionId="...")` 的 `(` 会被 `topParenOpen` 找到，
`methodNameBefore` 提取出 `MixinMerged` 而非实际方法名。

#### 4. 字段 vs 方法分类
按**结尾符**判断：
- `;` 结尾 → FIELD（无论体内含多少 `(`、`{}`、lambda）
- `}` 结尾 → METHOD/TYPE

之前按 `topParenOpen` 是否有 `(` 判断，导致
`APOLLYON = SynchedEntityData.defineId(Apostle.class, ...)` 这种带初始化调用的字段被误判为 METHOD。

#### 5. import 按使用过滤
- 只保留被修改方法 + 构造器垫片实际引用的类（词边界匹配）
- **同包 import 必须移除**：`import com.Polarice3.Goety.common.entities.boss.Apostle;`
  在骨架中冗余且触发 `Goety.class` 的包/类名冲突
- 未使用的 import（如 `javax.annotation.Nullable`）在目标类加载器中可能无法解析

#### 6. super 调用防递归
子类骨架里 `super.customServerAiStep()` 编译为 `invokespecial Sheep.m_8099_`；
移植回 `Sheep.customServerAiStep` 本体后变成调用自己 → 无限递归。
修复：拼接后把 INVOKESPECIAL + owner==目标类 + 非 `<init>` 的调用 → owner 改写为 `obfCN.superName`。

#### 7. 继承链成员重映射
javac 对 `this.eatBlockGoal`、`this.entityData` 等继承成员引用，owner 是**静态接收者类型**（子类 $$SSPatch）。
obfB 无法重映射（$$SSPatch 不在映射表）。修复：
- owner 改写：`$$SSPatch` → 目标类内部名
- 成员名改写：沿继承链（Sheep → Animal → … → Object）逐级配对 deobfCN ↔ obfCN，构建 `反混淆名+混淆desc → 运行时名` 表
- 私有方法调用：编译器看到放宽后的 public 发出 invokevirtual，运行时目标是 private → 修正为 invokespecial

#### 8. FileManager 搜索
- **不能用 `list()` 枚举**？可以，但必须处理类/包名冲突
- `Goety.class` 在 `com.Polarice3.Goety` 包中：类名 = 包名末段 → 跳过（isPackageShadow）
- `binaryName` 必须剥掉 `.class` 后缀（否则 inferBinaryName 返回错误名字 → 全部 import 失败）
- `getJavaFileForInput` 必须覆盖：从目标类加载器取字节 + 反混淆 + 放宽权限
  （否则 javac 补全注解符号时 CompletionFailure → 编译器崩溃，输出 GBK 乱码）

#### 9. 注解剥离（widenAccess）
HotCplr.widenAccess 除了开权限还要**剥掉全部注解**（类/方法/字段的可见+不可见+参数+类型）。
否则 javac 补全 `@Nullable` 等注解符号时 CompletionFailure → 编译器崩溃。

#### 10. 构造器处理
构造器不能在子类中以同名覆盖。方案：
- 改名为 `public void $$patchInit$N(params)` 编译
- `super(...)` 替换为对占位方法 `$$patchSuper$N(params)` 的调用
- 拼接后 `invokevirtual $$patchSuper$N` → `invokespecial <init>`
- `this(...)` 链式调用暂不支持

#### 11. 导入的包必须加入 allowedPackages（已移除，改为 getJavaFileForInput 方案）
早期用 allowedPackages 白名单过滤 buildPackageClasses 的搜索范围，
但对类型继承链（RangedAttackMob、Raider 等未 import 的父类/接口）不够宽 → NoSuchFileError。
最终方案：移除 list() 中的 allowedPackages，靠 getJavaFileForInput 按精确名取类。

#### 12. 成员扫描（addMember）注解剥离
分类和名字提取前先剥离注解行得到 `cls`，否则：
`@MixinMerged(mixin="...", sessionId="...")` 的 `(` 被 `topParenOpen` 找到，
方法名提取为 `MixinMerged` 而非实际方法名。

#### 13. static 方法引用
`invokestatic` 的方法如果 owner 被改写为目标类且方法是 private：
invokestatic 本身可以调私有方法（access check 通过），不需要特殊处理。

---

## 二、JEditScreen GUI

### 标签页系统
- 左侧类列表点击 → 反编译 → 可编辑标签页（带 Run `>` 按钮 + 关闭 `×`）
- Executor 标签页：自含代码编译并发送到服务端
- Tab 关闭时正确处理 activeTabindex 偏移

### 语法高亮（VSCode Dark+ 风格）
| 元素 | 颜色 |
|---|---|
| 控制流关键字 | #C586C0 紫 |
| 声明关键字/基本类型/字面量 | #569CD6 蓝 |
| 常量（ALL_CAPS） | #4FC1FF |
| 注解 | #DCDCAA |
| 类型名（大写开头） | #4EC9B0 青 |
| 方法（声明+调用） | #DCDCAA 黄 |
| 字段/变量 | #9CDCFE 浅蓝 |
| 字符串 | #CE9178 橙 |
| 注释 | #6A9955 绿 |
| 数字 | #B5CEA8 |

### 滚动条
- 点在拇指上 → 原位抓取（零跳变）
- 点在轨道上 → 跳到该处一次，随后相对拖动
- `dragGrabOffset` 记录鼠标-拇指偏移

### 滚轮速度
`scrollRate()` 覆写：`font.lineHeight * SCROLL_LINES`（原版 0.5 行/格 → 3 行/格）

### 行号宽度变化 bug
`setGutterWidth` 重建 `MultilineTextField` 时**必须重新注册** `setValueListener`——
否则行号宽度变化后所有键入不会同步到 `tab.content`（表现为 patch 报"无修改"）。

### Console 输出格式（编译器风格英文）
```
> method-patch com.Polarice3.Goety...Apostle
target: com.Polarice3.Goety...Apostle
diff: 1 modified method(s)
  - customServerAiStep()
skeleton com.Polarice3...Apostle$$SSPatch compiled OK (2841 bytes)
splice customServerAiStep(): OK -> m_8099_(...)
skipped 1 unsupported change(s):
  [skip] add member (retransform cannot add): METHOD foo()
retransform OK (24152 bytes, 1 method(s))
note: retransform is atomic - invalid bytecode keeps the original class

finished in 2.328s
```

编译错误格式（javac 风格）：
```
Sheep$$SSPatch.java:79:12: error: ';' expected
        System.out.ptintln("ciallo")
                                    ^
```

---

## 三、SSEnvLayer 特效

### 结构
- 遮罩（ENV_MASK）：以玩家相机为球心的暗色反穹顶，LEQUAL + 不写深度 → 只有天空透出
- 上浮方块（FLOAT_*）：随机原版方块从地面上浮，三轴自由旋转
- 三态：ENTER 渐深（无方块）→ ALIVE 全黑+方块 → EXIT 渐淡+方块统一缩小

### 关键注意点
- `isPackageShadow` 的类/包冲突检查必须保留
- 上浮时钟用 ALIVE 状态本地时钟（`stateTime`），不能用 `tickCount`（ENTER 期间也在走）
- EXIT 时用 `lastAliveClock` 断点续走，防止 y 跳变
- 渲染走 static 层管线（无 yaw 旋转，防抖动）
- `entity.getPosition(partialTick)` 不是 `entity.position()`（前者带插值）
- 方块渲染用 `renderSingleBlock` + 全亮度 `0xF000F0`

### ACTIVE 数组常量
```java
ENTER_ACTIVE[6] = 0   // 黑幕淡入开始
ENTER_ACTIVE[7] = 100 // 黑幕淡入结束
ALIVE_ACTIVE[0] = 2   // 代码雨上升一格最少tick（已闲置）
ALIVE_ACTIVE[1] = 4   // 代码雨上升一格最多tick（已闲置）
ALIVE_ACTIVE[2] = 60  // 方块入场错峰总时长
DEATH_ACTIVE[6] = 0   // 黑幕淡出开始
DEATH_ACTIVE[7] = 60  // 黑幕淡出结束
DEATH_ACTIVE[8] = 40  // 方块统一缩小消失tick
```

---

## 四、SSEnterLayer 入场特效

### 代码流（当前版本）
- 100 条径向射线，从 14-30 格外沿直线飞向球面（半径 2F）
- 逐字符变轨：头字符先着陆，沿弧滑向槽位（指数衰减 glide）
- 着陆点即穿越点（方位角 yaw0），着陆后开始各自圆周运动
- super 调用改写至真实父类防递归
- 字符直绘用 BakedGlyph.render + NO_CULL RenderType（绕过文字 cull）
- 字体纹理绑定：反查 BakedGlyph.renderTypes 对应的 FontTexture（运行时拼接图集）
- 双本体夹描边：本体 → 4向白描边 → 本体（提交顺序即绘制顺序，sortOnUpload=false）
- 描边偏移 0.3px，通过 OUTLINE_DIRS 控制

### 关键修复记录
| 问题 | 根因 | 修复 |
|---|---|---|
| 文字不可见 | 文字 RenderType 默认 CULL | NO_CULL + 直绘 BakedGlyph |
| 黑紫块 | 纹理路径指向不存在的静态 PNG | 绑定运行时 FontTexture |
| CompletionFailure 崩溃 | getJavaFileForInput 未覆盖 | 覆盖从目标加载器取类 |
| NoSuchFieldError | 继承链成员名未重映射 | 继承链 deobf↔obf 配对表 |
| StackOverflowError | super 调用指向自身 | 改写到 obfCN.superName |
| NoSuchFieldError: entityData | 重映射表只覆盖目标类 | 沿继承链走完整条链 |

---

## 五、调试基础设施

### 隔离测试
不启动游戏即可测试编译管线：
1. 用 Write 写独立 Java（javax.tools + ASM + forge mapped jar）
2. javac 编译 → java 运行
3. 验证 import 解析、权限放宽、方法覆盖
4. 测试台在 `%TEMP%\patcher_test`

### 调试转储（当前注释状态，取消 // 即可启用）
| 注释行 | 内容 |
|---|---|
| `//dumpFile("00_compile_error.txt", ...)` | 编译错误详情 |
| `//dumpFile("01_skeleton.java", ...)` | 骨架源码 |
| `//dumpFile("02_skeleton_deobf.class", ...)` | 骨架编译产物 |
| `//dumpFile("03_target_runtime.class", ...)` | 目标类原始字节 |
| `//dumpFile("04_target_deobf.class", ...)` | 目标类反混淆字节 |
| `//dumpFile("05_skeleton_obf.class", ...)` | 骨架混淆映射字节 |
| `//dumpFile("06_remap_tables.txt", ...)` | 重映射表 |
| `//dumpFile("07_patched.class", ...)` | 最终拼接类 |
| `//dumpFile("08_report.txt", ...)` | 完整报告 |

转储目录：`sspatch_debug/`（游戏工作目录下）
日志前缀：`[SSPatch]`

---

## 六、已知限制

1. retransform 不能增/删方法/字段/改签名 → 新增/删除/字段修改被拒绝
2. 抽象方法不能通过方法补丁实现（会改变类结构）
3. 构造器 `this(...)` 链式调用暂不支持
4. 含 `invokedynamic`（lambda）的方法可能需要完整类编译
5. CFR 的 `/* Unavailable Anonymous Inner Class!! */` 伪影无法自动修复
6. 实体追踪距离限制：超出后客户端没有实体，补丁不生效
7. javac 补全符号时如果引用了 classpath 上不存在的注解类 → CompletionFailure → 崩溃（已通过剥注解修复）

---

## 七、关键 AT（accesstransformer.cfg 追加项）

```
public-f net.minecraft.client.gui.font.glyphs.BakedGlyph
public-f net.minecraft.client.gui.font.glyphs.BakedGlyph *
public-f net.minecraft.client.gui.font.glyphs.BakedGlyph *()
public-f net.minecraft.client.gui.font.FontSet
public-f net.minecraft.client.gui.font.FontSet *
public-f net.minecraft.client.gui.font.FontSet *()
public-f net.minecraft.client.gui.font.FontTexture
public-f net.minecraft.client.gui.font.FontTexture *
public-f net.minecraft.client.gui.font.FontTexture *()
```

---

## 八、她的关键提示汇总

1. "不修改只推荐一种环境效果最符合场景"
2. "支持自定义渲染（想画啥画啥）"
3. "放弃思考存在动画，现在看入场动画"
4. "出场四周会有多条短但是多的ascii随机字符条目（渐变彩色）向中心汇聚（由透明到不透明）"
5. "直接修改"
6. "不要让文字面向屏幕，使用条带式向中心汇聚"
7. "java源码，参考：D:\Programs\Minecraft\.superminecraft\versions\1.20.1F\!!!source"
8. "不是这样，我需要字一头始终朝向ss，一头朝向对面，字进行梯形变形向ss汇聚"
9. "不要到处转，只点到点"
10. "再远一点"
11. "字再明显，有白色描边"
12. "不需要注解，font直接用mc.font"
13. "代码由更远处向中心（半径和方块一致）汇聚并旋转最终和方块一起收缩向中心"
14. "现在这个效果是一边旋转一边收缩，我要的是先汇集再以文字为筒壁做圆周运动在方块合并时一样合并"
15. "描边薄一点"
16. "运动效果还是有问题，首先让文字汇集速度更快（量可以变大），其次在圆周运动时文字不是整条当作圆弧运动的，我需要整条按照圆弧运动每个速度可以不一样"
17. "总体还是球体但是运动规则按照我说的"
18. "不要同时汇聚，分条汇聚，汇聚时不转动"
19. "来的时候是一端向ss一端反向而不是横着飘过去，其次，汇聚的时候太规整了"
20. "不是这样，汇聚的时候是头对ss尾反向整体直线，沿直线到ss指定半径球上就自动变换为球体形态"
21. "文字剑都成曲线进入"
22. "重新读取，这是我大概理想中的代码，备份，为飞到球体边缘再到旋转缩小球体这个转换做更好的更流程的衔接动画"
23. "飞到球体边缘时直接改运动方向、朝向呗，为什么还要到中心经过一些我看不清的操作"
24. "每条切入球体的间隔跨度再稍稍大一点，切入球体后在原位立刻变轨吗"
25. "变换的球体半径大一点"
26. "逐字符变轨，虽然更难但更好看 继续"
27. "增加缩小后球体半径字段"
28. "删除所有注解"
29. "出场后天空变黑怎么实现"
30. "这样，新建SSEnvLayer，在玩家摄像机制作 gl对比始终在下的遮罩让玩家摄像机始终渲染，先实现我会尝试，避免使用事件"
31. "isStatic为true即可防止抖动，只在ALIVE时启用"
32. "为什么一直在抖"
33. "我修改了，再次读取，仿照之前的代码流写从天而下的代码雨，只围绕本mc玩家下，但是代码从下向上"
34. "甚至看不见代码雨"
35. "因为文字cull的原因请直接绘制ascii表上的文字以绕过cull限制，并且这个罩子好像会罩住字"
36. "不需要注解，font直接用mc.font"
37. "代码由更远处向中心（半径和方块一致）汇聚并旋转最终和方块一起收缩向中心"
38. "现在的效果是一边旋转一边收缩，我要的是先汇集再以文字为筒壁做圆周运动在方块合并时一样合并"
39. "描边薄一点"
40. "运动效果还是有问题..."
41. "总体还是球体但是运动规则按照我说的"
42. "不要同时汇聚，分条汇聚，汇聚时不转动"
43. "来的时候是一端向ss一端反向而不是横着飘过去，其次，汇聚的时候太规整了"
44. "不是这样，汇聚的时候是头对ss尾反向整体直线，沿直线到ss指定半径球上就自动变换为球体形态"
45. "文字剑都成曲线进入"
46. "重新读取，这是我大概理想中的代码，备份..."
47. "飞到球体边缘时直接改运动方向、朝向呗..."
48. "每条切入球体的间隔跨度再稍稍大一点，切入球体后在原位立刻变轨吗"
49. "变换的球体半径大一点"
50. "逐字符变轨，虽然更难但更好看 继续"
51. "增加缩小后球体半径字段"
52. "删除所有注解"
53. "出场后天空变黑怎么实现"
54. "这样，新建SSEnvLayer..."
55. "isStatic为true即可防止抖动，只在ALIVE时启用"
56. "为什么一直在抖"
57. "我修改了，再次读取..."
58. "甚至看不见代码雨"
59. "因为文字cull的原因请直接绘制ascii表上的文字以绕过cull限制..."
60. "不需要注解，font直接用mc.font"
61. "代码由更远处向中心（半径和方块一致）汇聚并旋转最终和方块一起收缩向中心"
62. "现在这个效果是一边旋转一边收缩..."
63. "描边薄一点"
64. "运动效果还是有问题..."
65. "总体还是球体但是运动规则按照我说的"
66. "不要同时汇聚，分条汇聚，汇聚时不转动"
67. "来的时候是一端向ss一端反向..."
68. "不是这样，汇聚的时候是头对ss尾反向整体直线..."
69. "文字剑都成曲线进入"
70. "重新读取，这是我大概理想中的代码，备份..."
71. "飞到球体边缘时直接改运动方向..."
72. "每条切入球体的间隔跨度再稍稍大一点..."
73. "变换的球体半径大一点"
74. "逐字符变轨..."
75. "增加缩小后球体半径字段"
76. "删除所有注解"
77. "出场后天空变黑怎么实现"
78. "新建SSEnvLayer..."
79. "isStatic为true..."
80. "为什么一直在抖"
81. "仿照之前的代码流写从天而下的代码雨..."
82. "甚至看不见代码雨"
83. "因为文字cull的原因..."
84. "不需要注解，font直接用mc.font"
85. "代码由更远处向中心..."
86. "现在这个效果..."
87. "描边薄一点"
88. "运动效果..."
89. "总体还是球体..."
90. "不要同时汇聚..."
91. "来的时候..."
92. "不是这样..."
93. "文字剑都成曲线进入"
94. "重新读取...备份..."
95. "飞到球体边缘时..."
96. "每条切入球体的间隔..."
97. "变换的球体半径大一点"
98. "逐字符变轨..."
99. "增加缩小后球体半径字段"
100. "删除所有注解"
101. "出场后天空变黑怎么实现"
102. "新建SSEnvLayer..."
103. "isStatic为true..."
104. "为什么一直在抖"
105. "我修改了..."
106. "甚至看不见代码雨"
107. "因为文字cull的原因..."
108. "描边影响中间文字了..."
109. "不是这个意思，是让两个本体和中间四个描边形成面包夹芝士..."
110. "描边还有只是让本体偏移绘制"
111. "patch的原理改为将被修改方法提到一个继承于该类的类中..."
112. "你可以自己新建java去试试，先将热编译器的搜索资源恢复到我的版本（保留开权限）"
113. "增加鼠标滚轮在所有text窗口的滑动速度"
114. "将所有在gui console输出标准化，更像一个编译器，输出都用英文"
115. "第一个文件改为第二个文件我删了个注解就..."
116. "我关不掉标签页了，在修改完gui之后"
117. "参考我之前给的apostel示例..."
118. "你直接在编译完子类之后把子类混淆了不就得了..."
119. "增加鼠标滚轮..."
120. "将所有在gui console输出标准化..."
121. "第一个文件改为第二个文件..."
122. "我关不掉标签页了..."
123. "参考我之前给的apostel示例，我修改了revelaionfix$apollyonGH() {的注解然而他却判定其他方法被修改了"
124. "在某个地方输出一下中间类代码和一些日志，我最后再给你你更好判断你哪里有问题"
125. "没有任何SSPatch输出..."
126. "可用，注解掉所有写入文件/调试输出逻辑"
127. "是注解不是删除"
128. "输出了'...'这样的东西，继续修改"（CompletionFailure 崩溃）
129. "写个py脚本将...jedit.png保存到新文件并将所有63d5ff颜色的块保持alpha不变将颜色更改为008aeb"
130. "导出MD，包含特效修改之后的所有注意点、我的关键提示、处理时的重要点"
