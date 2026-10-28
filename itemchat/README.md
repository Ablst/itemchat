# ItemChat

一个 Minecraft **NeoForge 1.21.1** 模组：对着物品按【鼠标中键】就能在聊天栏向所有玩家展示它，并附一个可点击的 **[检查]** 按钮，点一下直接跳到 JEI 里该物品的配方界面；对着 FTB 任务按中键则会展示该任务，并附 **[检查任务]** 按钮回跳任务书。

> 本项目由 Forge 1.20.1 版本移植而来，当前为 **1.2.2**。
> 注意：本号是 NeoForge 1.21.1 这条线**自己的**版本序列，与 Forge 1.20.1 线（`itemchat-forge`）的版本号互相独立、不跟随。

## 功能

| 操作 | 结果 |
| --- | --- |
| 在背包 / 箱子 / 任意容器界面里，鼠标悬停在物品上按**中键** | 聊天栏广播：`[物品] 玩家名 展示了 钻石剑 ×64 [检查]` |
| 在 **JEI 物品列表**（容器界面右侧浮层、配方界面右列、书签栏）里对物品按**中键** | 同上，分享 JEI 里悬停的那个物品 |
| 在 **FTB 任务界面**里对着一条任务按**中键** | 聊天栏广播：`[任务] 玩家名 展示了 某任务 [检查任务]` |
| 鼠标悬停在消息里的物品名上 | 显示该物品的完整 tooltip（和背包里看到的一样） |
| 点击物品名或 `[检查]` | 打开 JEI 中该物品的配方界面（等价于在 JEI 里按 R） |
| 点击 `[检查任务]` | 打开 FTB 任务书并**定位到那条任务** |

- 广播消息由**服务端**生成，因此**所有玩家**都能看到悬停预览和可点击按钮。
- JEI 和 FTB 任务都是**可选**依赖：没装时其他功能照常，只有对应按钮会提示未检测到。
- 消息文本全部走翻译键，**跟随每个玩家自己的游戏语言**（内置 `en_us` / `zh_cn`）。
- 服务端没装本模组时会自动降级，只在你自己的客户端提示一句，不会报错，也**不会阻止你进入原版服务器**。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.259（21.1.x 均可） |
| Java | **21**（编译和运行都需要） |
| JEI | 19.x（可选） |
| FTB Quests | 2101.x（可选；装了它还需要 FTB Library / FTB Teams / Architectury） |

## 构建

```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

产物在 `build/libs/itemchat-1.2.2-neoforge.jar`。

命名规则是 `<modid>-<modversion>-<loader>.jar`：NeoForge 版带 `-neoforge` 后缀，
和 Forge 版的 `itemchat-1.3.0-forge.jar` 区分开，便于按所用加载器选择正确文件；两版不能混装。

在 IDE 里调试：

```bash
gradlew.bat runClient                 # 启动带模组的客户端（已包含 JEI + FTB，方便测试）
gradlew.bat runServer                 # 启动专用服务端
gradlew.bat runServer -PwithFtb=false # 不加载 FTB，用来验证「没装 FTB 也不崩」的软依赖降级
```

> 首次构建会下载 NeoForge / Minecraft / FTB 依赖，视网络情况需要几分钟到十几分钟，属正常现象。

仓库里预置了几个国内网络环境需要的绕行配置：

| 位置 | 配置 | 作用 |
| --- | --- | --- |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` 指向腾讯云镜像 | `services.gradle.org` 在国内经常连不上 |
| `settings.gradle` | `gradle.beforeProject` 把 `maven.neoforged.net` 改写到 CDN77 镜像 | ModDevGradle 会硬编码注册官方仓库，而该域名在国内不可达 |
| `build.gradle` 的 `repositories` | 阿里云 `public`、Maven Central、BlameJared、FTB、Architectury | 兜住传递依赖、JEI 和 FTB 全家桶 |

**JDK 需要你自己提供**：本项目要求 Java 21，但仓库里**刻意不写死任何本机路径**
（写死会让别人 clone 下来直接构建失败）。任选一种方式：

```bash
# 方式一：设置 JAVA_HOME 指向 JDK 21
export JAVA_HOME=/path/to/jdk-21     # Windows: set JAVA_HOME=C:\path\to\jdk-21
./gradlew build

# 方式二：写进你自己的 ~/.gradle/gradle.properties（不会被提交）
#   org.gradle.java.home=/path/to/jdk-21
```

> 只设 `org.gradle.java.installations.paths` 是不够的 —— 它只影响编译用的工具链，
> 管不到 Gradle 守护进程本身。ModDevGradle 要求守护进程跑在 17+，否则 apply 插件时
> 直接报 `please run Gradle with Java 17 or newer`。
> 如果你的 JDK 不在 Gradle 能自动探测的位置，再补一行 `org.gradle.java.installations.paths`。

## 安装

**客户端**：把 `itemchat-1.2.2-neoforge.jar` 放进 `.minecraft/mods/`。

**服务端**：同一个 jar 也放进服务端的 `mods/` 目录。**这一步是必须的** —— 广播消息需要服务端构造富文本组件，只有客户端装了的话，别人是看不到按钮的（游戏会提示“当前服务器没有安装 ItemChat”）。想用 FTB 任务分享功能的话，服务端也需要装 FTB 任务。

单人游戏不需要额外操作：内置服务端会加载同一个 jar。

## 使用

1. 打开背包（按 `E`）或任意容器。
2. 鼠标移到想展示的物品上。
3. 按**鼠标中键**。

或者在 JEI 的物品列表里对着物品按中键，在 FTB 任务书里对着任务按中键。

想改默认按键？目前中键是写死在 `ClientEvents#onMouseButtonPre` 里的（不占用原版按键绑定），
把那里的 `GLFW.GLFW_MOUSE_BUTTON_MIDDLE` 改成别的即可。

## 代码结构

```
src/main/java/com/example/itemchat/
├── ItemChat.java                  主类：挂网络注册 + 提供 tr() 本地化助手
├── net/
│   ├── ItemChatNetwork.java       PayloadRegistrar 注册四个 CustomPacketPayload
│   ├── ShareItemPacket.java       客户端 -> 服务端：分享物品
│   ├── OpenJeiPacket.java         服务端 -> 客户端：请点击者打开 JEI
│   ├── ShareQuestPacket.java      客户端 -> 服务端：分享 FTB 任务
│   └── OpenQuestPacket.java       服务端 -> 客户端：请点击者打开任务书
├── server/
│   ├── ItemBroadcaster.java       构造物品富文本并广播
│   ├── ItemChatCommands.java      服务端命令 /itemchat jei|quest
│   ├── QuestBroadcaster.java      FTB 软依赖守卫层（没装时降级提示）
│   └── FtbQuestServerBridge.java  服务端侧真正接触 FTB 类型的代码
└── client/
    ├── ClientEvents.java          统一的中键捕获（InputEvent.MouseButton.Pre）
    ├── ClientPayloadHandler.java  处理 OpenJei / OpenQuest
    ├── JeiIntegration.java        JEI 软依赖桥接（取悬停物品 / 打开配方）
    ├── FtbQuestsIntegration.java  FTB 软依赖桥接（取悬停任务 / 打开任务书）
    └── ItemChatJeiPlugin.java     JEI 插件入口，拿到 IJeiRuntime

src/main/resources/assets/itemchat/lang/
├── en_us.json                     15 个翻译键
└── zh_cn.json                     同上
```

## 工作原理

物品链路：

```
[客户端] 中键点击槽位
   └─ ShareItemPacket ──► [服务端] ItemBroadcaster
                              └─ 生成带 HoverEvent / ClickEvent 的 Component
                                 └─ 广播给所有玩家
                                       └─ [客户端] 点击 [检查] 按钮
                                             └─ 原版发送命令 /itemchat jei minecraft:diamond_sword
                                                   └─ [服务端] ItemChatCommands 校验执行者
                                                         └─ OpenJeiPacket ──► [客户端] ClientPayloadHandler
                                                                                 └─ 打开本机 JEI 的配方界面
```

任务链路完全对称，只是包换成 `ShareQuestPacket` / `OpenQuestPacket`，命令换成
`/itemchat quest <16位十六进制任务id>`，客户端打开的是 FTB 任务书。

八个容易踩坑的地方，这里都处理了：

1. **为什么必须走自定义网络包？**
   玩家自己发出去的聊天只有纯文本，服务端不会转发 `ClickEvent`。只有服务端构造并下发的组件，
   才能让所有人都看到按钮。

2. **为什么“检查”按钮要绕一圈交给服务端？**
   Minecraft 的点击事件只支持 `open_url` / `run_command` / `suggest_command` /
   `copy_to_clipboard` / `change_page` 五种。按钮用的是 `run_command`，而原版
   `Screen#handleComponentClicked` 处理 `RUN_COMMAND` 时走的是 **`sendUnsignedCommand`**，
   客户端注册的命令根本拦不到，命令会被原样发到服务端并报“未知或不完整的命令”。
   所以现在改成：按钮执行**服务端命令** `/itemchat jei <物品ID>`，服务端校验后回一个
   `OpenJeiPacket` 给点击者，由客户端本地打开 JEI。这样每个点击的玩家打开的都是自己客户端的 JEI。

3. **中键为什么用 `InputEvent.MouseButton.Pre`，而不是 `ScreenEvent`**
   最早用的是 `ScreenEvent.MouseButtonPressed.Pre`，它是在 `Screen#mouseClicked` 那条链上发的，
   很多模组都会监听，而且一旦有人先 `setCanceled(true)`，事件系统就不会再调用后面那些
   没声明 `receiveCanceled` 的监听器。实测撞上过：同一份 jar 在 JEI 15.20 的整合包里中键正常，
   在 JEI 15.49 的整合包里就没反应。
   改成 `InputEvent.MouseButton.Pre` + 优先级 `HIGHEST` 之后，这个事件在
   `ClientHooks#onMouseButtonPre` 里、**把点击分发给屏幕之前**触发，取消后屏幕（包括 JEI 浮层）
   根本收不到这次点击，从根上绕开了“谁先取消”的竞争。

4. **网络通道的可选性**
   注册时用了 `PayloadRegistrar#optional()`（等价于旧版的 `NetworkRegistry.acceptMissingOr`）
   而不是严格版本校验。如果用严格校验，NeoForge 会要求对端也必须存在这个通道，结果就是
   **装了本模组之后再也进不去原版服务器**。用了 `optional()` 之后，对端没有本模组时连接照常建立，
   只是功能降级（`ClientEvents` 里会按包类型分别判断通道在不在，不在就只给本地提示）。

5. **退出 JEI / 任务书后不要停在聊天输入状态**
   点“检查”时聊天界面是开着的，而原版处理 `RUN_COMMAND` 点击只发命令、**不关闭聊天界面**；
   JEI 的 `RecipesGui#open()` 又会把“打开配方界面前的那个界面”记进 `parentScreen`，退出时切回去。
   两者叠加的结果就是：退出 JEI 后回到“按 T 呼出鼠标”的聊天输入状态，得再按一次回车 / Esc。
   所以 `ClientPayloadHandler` 在调用打开入口 **之前** 先把聊天界面关掉
   （`Minecraft.setScreen(null)`），这样记下的父界面是 `null`，退出后直接回到游戏画面。

6. **两个第三方依赖都是软依赖，不能有半句硬引用**
   `ClientEvents` / `ClientPayloadHandler` / `ItemChatCommands` 以及数据包这些**可能被服务端加载**的类，
   一律不 `import` JEI 或 FTB 的类型；真正接触第三方类型的代码全部关在
   `JeiIntegration` / `FtbQuestsIntegration` / `FtbQuestServerBridge` 里，调用前先过
   `ModList.get().isLoaded("jei" | "ftbquests")` 守卫。这样没装它们的客户端 / 服务端
   永远不会因为缺类而抛 `NoClassDefFoundError`。

7. **FTB 的 `getTitle()` 在服务端和客户端不是一回事**
   FTB 源码（`QuestObjectBase.java`）里是这样写的：

   ```java
   cachedTitle = !getRawTitle().isEmpty()
       ? (getQuestFile().isServerSide()
           ? Component.literal(getRawTitle())                       // 服务端：纯字面量
           : TextUtils.parseRawText(getRawTitle(), holderLookup())) // 客户端：解析
       : getAltTitle();
   ```

   服务端拿到的是**原样字符串**，既不翻译也不解析 `&a`/`&6` 这类颜色代码；只有客户端才会
   用本机语言去查 FTB 自己的 locale 表（`config/ftbquests/quests/lang/<语言>.snbt`，**不是**原版的
   `Language`）并解析颜色代码。所以任务名必须由**点中键的那个客户端**渲染好再随包发上去，
   服务端只校验任务存在并广播 —— 否则就会出现"中文客户端看到 `&aGeneral Storage Options`"
   这种"英文 + 裸颜色代码"的怪相。
   客户端传上来的组件属于不可信输入，服务端在广播前会剥掉其中的 clickEvent / hoverEvent /
   insertion，只留颜色这类视觉样式。

8. **任务书里的"任务"有两种对象，`instanceof Quest` 认不全**
   `QuestPanel` 建按钮时其实分三种：`QuestButton`（真任务）、`QuestLinkButton`（任务链接）、
   `ChapterImageButton`（章节装饰图）。其中任务链接是 FTB 为了让同一条任务能出现在多个章节里
   而自动生成的隐形对象 —— **外观和普通任务完全一样，但它有自己的 id，和被链接的任务不是同一个
   id**，所以只判 `instanceof Quest` 会把点中链接的情况判成"任务不存在"。
   另外 FTB 在**网络同步** `QuestLink` 时把 `linkId`（指向的目标）写死成 `0L`
   （`BaseQuestFile#readNetData`），也就是说**客户端的链接对象追不到目标**，这件事只能由服务端做
   （服务端的链接是从 SNBT 读的，`linkId` 完整）。所以现在的分工是：
   客户端把按钮对应的对象 id 原样发上去 + 用 `Widget#getTitle()` 取按钮标题
   （链接按钮构造时传进去的就是被链接的那条任务，所以标题天然正确）；
   服务端顺着 `QuestLink#getQuest()` 追到真任务再广播。装饰图则直接当没命中。

> 客户端类只在客户端被加载：`OpenJeiPacket` / `OpenQuestPacket` 注册为 `playToClient`，
> 它们的处理器在专用服务端上永远不会被触发，所以直接调用客户端处理类也是安全的
> （NeoForge 已经删掉了 `DistExecutor`，不再需要它兜底）。

## 从 Forge 1.20.1 移植的主要改动

| 位置 | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| 构建插件 | ForgeGradle 6 | **ModDevGradle 2.0.148** |
| 元数据文件 | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml`（依赖用 `type="required/optional"`） |
| Java | 17 | 21 |
| 网络 | `SimpleChannel` + `registerMessage` | `CustomPacketPayload` + `PayloadRegistrar` + `StreamCodec` |
| 发包 | `CHANNEL.send(PacketDistributor.PLAYER.with(...), p)` | `PacketDistributor.sendToServer / sendToPlayer` |
| 通道检测 | `SimpleChannel#isRemotePresent` | `ICommonPacketListener#hasChannel` |
| 注册表 | `ForgeRegistries.ITEMS` | `BuiltInRegistries.ITEM` |
| 事件总线 | `Bus.FORGE` / `Bus.MOD` | `Bus.GAME` / `Bus.MOD`（已废弃显式 `bus` 属性，FML 按事件类型自动路由） |
| 物品数据 | `ItemStack#getTag()`（CompoundTag） | 组件系统（`DataComponentPatch`） |
| 客户端分发 | `DistExecutor.unsafeRunWhenOn` | 不需要（`playToClient` 处理器只在客户端执行） |
| ResourceLocation | `new ResourceLocation(...)` | `ResourceLocation.fromNamespaceAndPath(...)` |
| FTB 依赖 | `2001.x` + item-filters | `2101.x`（**不再需要 item-filters**） |

## 1.2.2 更新内容

- 修复：中键分享**任务链接**（FTB 为了让同一条任务能出现在多个章节里而自动生成的隐形链接对象）
  时会报"找不到这条 FTB 任务"。任务链接在任务书里看起来和普通任务一模一样，但它有自己的 id，
  和被链接的那条任务不是同一个 id，之前的校验只认 `Quest`，于是直接判成"不存在"。
  现在客户端把对象 id 原样发上去，服务端会顺着链接追到真正的那条任务（`[检查任务]` 按钮也指向它），
  章节装饰图这类本来就不是任务的对象则直接忽略，不再报错。
- 变更：`1.2.1` 的网络协议版本 `"2"` 不变（本次没有改包结构）。

## 1.2.1 更新内容

- 修复：中键分享 FTB 任务时，任务名显示的是**英文原文 + 裸颜色代码**（例如
  `&aGeneral Storage Options`），而不是任务书里那一行。原因见上面第 7 条的说明 ——
  服务端的 FTB `getTitle()` 只返回原样字面量。现在改成由分享者自己的客户端按本机语言
  渲染好标题再发上去，颜色代码也会被正确解析。
- 变更：`ShareQuestPacket` 增加了任务名字段，**网络协议版本从 `"1"` 提到 `"2"`**。
  新旧版本混用时对应功能会优雅降级（连接不会断，只是提示"未检测到"），不会出现解析错乱。

## 1.2.0 更新内容

- 新增：中键 **JEI 物品列表 / 配方界面 / 书签栏**里的物品也能分享到聊天框。
- 新增：中键 **FTB 任务**分享到聊天框，并带 `[检查任务]` 按钮回跳任务书。
- 修复：中键捕获改用 `InputEvent.MouseButton.Pre`（优先级 `HIGHEST`），
  修掉“某些整合包里中键 JEI 没反应”的问题。
- 新增：完整本地化，所有玩家可见文本走翻译键，不再硬编码中文。
- 新增：`assets/itemchat/lang/` 下的 `en_us.json` / `zh_cn.json`。
  想加别的语言，往那个目录丢一个 `<语言代码>.json` 翻一遍即可，不用改代码。
- 变更：作者名改为 `Abslt_`。
- 变更：模组列表里的描述改成英文。这是不得不为——元数据里的 `description` 是静态字符串，
  没有翻译机制，不随游戏语言变化。

## 已知限制

- 消息里传给 JEI 的只有物品 ID，所以附魔书、药水这类“同名不同组件”的物品，点开 JEI 后看到的是
  基础物品的配方，而不是那一份具体组件的。
- 组件数据特别大的物品（例如装满东西的潜影盒）在发包前会丢掉额外组件，避免超过自定义数据包
  约 32KB 的上限，消息里只显示物品本身和数量。
- 在容器界面按中键会取消原版行为，因此创造模式下“中键克隆物品”在背包里不再生效。
- 模组列表里的**描述**无法跟随游戏语言（见上）。
- 任务名用的是**分享者**客户端的语言。这是受限于「一条服务端广播消息要能被所有人看懂」这个
  前提：FTB 的翻译表不在原版 `Language` 里，无法做成"每个玩家各看各的语言"的延迟翻译组件
  （自定义组件类型又会让没装本模组的客户端读不出来）。物品名不受此限，它走的是原版
  `getHoverName()`，仍然是每人各自的翻译。

## 改名字

模组 ID、包名、作者等信息都在 `gradle.properties` 里，改完记得同步修改
`src/main/java/com/example/itemchat/` 的目录名和 `package` 声明，以及 `ItemChat.MOD_ID`。

## 许可证

本项目以 **GNU General Public License v3.0（GPL-3.0）** 发布，完整条款见本工程目录中的 [LICENSE](LICENSE)。

你可以使用、修改和再分发本项目，包括商业使用。分发原版或修改版时，须遵守 GPL v3：保留适用的版权和许可证声明，并随附许可证；分发修改版时还须说明修改情况和日期，并按 GPL v3 授权整个受其约束的修改作品。

如果分发编译后的 jar，须按照 GPL v3 第 6 条规定的方式，向接收者提供与该版本对应的完整源码（包括构建所需的脚本）。仅自己修改和使用、不向他人分发时，不要求公开源码；提供源码也不等于必须上传 GitHub。本模组按许可证规定不提供担保。

以上是便于理解的摘要，以 LICENSE 的完整条款为准。Minecraft 和第三方依赖的权利与许可由各自权利人规定，本项目的 GPL 声明不替代其许可，也不自动解决依赖之间的许可兼容性问题。
