# ItemChat —— Forge 1.20.1 版

一个 Minecraft **Forge 1.20.1** 模组：在物品上按【鼠标中键】，就能在聊天栏向所有玩家展示它，
并附一个可点击的 **[检查]** 按钮，点一下直接跳到 JEI 里该物品的配方界面。

> 当前版本 **1.3.0**（作者 Abslt_）。本工程是 Forge 1.20.1 线，产物为 `itemchat-1.3.0-forge.jar`。
> NeoForge 1.21.1 线是另一个独立工程（目录 `itemchat`），版本 1.2.2，产物 `itemchat-1.2.2-neoforge.jar`。

## 功能

| 操作 | 结果 |
| --- | --- |
| 容器界面里悬停在物品上按**中键** | 广播 `[物品] 玩家名 展示了 钻石剑 ×64 [检查]` |
| 悬停在消息里的物品名上 | 显示该物品的完整 tooltip |
| 点击物品名或 `[检查]` | 打开 JEI 中该物品的配方界面（等于在 JEI 里按 R） |
| **JEI 物品管理器里悬停在物品上按中键** | 同样广播该物品（支持物品列表 / 配方界面 / 书签栏） |
| **FTB 任务界面里悬停在任务上按中键** | 广播 `[任务] 玩家名 展示了 任务标题 [检查任务]` |
| 点击 `[检查任务]` | 打开 FTB 任务书并定位到该任务 |

- 广播消息由**服务端**生成，所以**所有玩家**都能看到悬停预览和可点击按钮。
- **所有对玩家显示的文字都跟随游戏语言**（见下）。
- **JEI 与 FTB 任务都是可选依赖**：没装时其它功能照常，只有对应的按钮会提示"未检测到 …"。
- 服务端没装本模组时会自动降级，只在你自己的客户端提示一句，不会报错，也**不会阻止你进入原版服务器**。
- FTB 任务需要**服务端也装**：任务标题得由服务端从任务文件里查出来才能广播。

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | **≥ 47.3**（本工程按 47.4.10 编译；FTB Library 2001.2.13 强制要求 `[47.3,)`） |
| Java | **17**（编译和运行都需要） |
| JEI | 15.x（可选，本工程按 15.49.0.188 编译） |
| FTB 任务 | 2001.x（可选，本工程按 2001.4.22 编译） |

## 本地化（i18n）

运行时出现的每一句话都走 `Component` 翻译键，语言文件在 `assets/itemchat/lang/`：

| 文件 | 语言 |
| --- | --- |
| `en_us.json` | 英文 |
| `zh_cn.json` | 简体中文 |

- 装了本模组的客户端按**它自己的**游戏语言显示（服务端只负责构造可翻译组件，翻译是在客户端做的，所以不同玩家可以各看各的语言）。
- 没装本模组的客户端会看到代码里写的 `fallback`（中文原文），**不会**看到裸的翻译键名 —— 用的是 `Component.translatableWithFallback`。

想加别的语言，往 `assets/itemchat/lang/` 里丢一个 `<语言代码>.json`、把这两个文件的内容翻过去就行。

> **例外：模组列表里的"描述"不跟随语言。** 那是 `META-INF/mods.toml` 里的静态元数据 ——
> Forge 1.20.1 的 `IModInfo#getDescription()` 返回的就是 `String`，没有翻译机制，
> 所以这里写成英文（也和语言无关的元数据惯例一致）。

## 构建时的一个坑：`filteringCharset`

`processResources` 里做 `expand`（替换 `mods.toml` 的 `${...}` 占位符）时，Gradle 默认用**平台编码**
读文件。中文 Windows 上是 GBK，拿它去读 UTF-8 的 `mods.toml` 会把中文拆坏 ——
表现就是模组列表里那一片 `?？` 乱码。所以 `build.gradle` 里显式写了：

```groovy
filteringCharset = 'UTF-8'
```

<br>

## 构建

```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

产物在 `build/libs/itemchat-1.3.0-forge.jar`。

命名规则是 `<modid>-<modversion>-<loader>.jar`：Forge 版带 `-forge` 后缀，
和 NeoForge 版的 `itemchat-1.2.2-neoforge.jar` 区分开，便于按所用加载器选择正确文件；两版不能混装。

在 IDE 里调试：

```bash
gradlew.bat runClient    # 启动带模组的客户端（已含 JEI + FTB 任务，方便测试）
gradlew.bat runServer    # 启动专用服务端
```

## 这个工程是怎么来的

原先的 Forge 1.20.1 源码在把同一目录移植成 NeoForge 1.21.1 时被覆盖丢失了
（当时目录既没有 git 也没有备份），只剩成品 `itemchat-1.0.0.jar`。
本工程是从那个 jar **反编译还原**出来的：10 个类逐一还原并补齐中文注释，
构建脚本按 ForgeGradle 6 官方 MDK 结构重写，然后在 1.0.0 的基础上新增了 JEI / FTB 两个中键功能。

反编译得到的 SRG 方法名（`m_41786_`、`m_131136_` 等）都对照
`~/.gradle/caches/forge_gradle/.../srg_to_official_1.20.1.tsrg` 翻译成了官方名
（例如 `[检查]` 按钮原本用的是 `withBold(true)`，也照原样还原）。

## 构建环境的几个坎

仓库里预置了国内网络环境需要的绕行配置：

| 位置 | 配置 | 作用 |
| --- | --- | --- |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` 指向腾讯云镜像 | `services.gradle.org` 在国内经常连不上 |
| `gradle.properties` 的 `systemProp.java.net.useSystemProxies` | `true` | **Gradle 不会自动使用 Windows 系统代理**；开着代理时它才会走代理，关了就走直连 |
| `build.gradle` 的 `repositories` | MinecraftForge、FTB（`maven.ftb.dev/releases`）、Architectury、BlameJared | JEI 和 FTB 全家桶都从各自官方 maven 拉取，**仓库里不 vendors 任何第三方 jar** |

**JDK 需要你自己提供**：本项目要求 Java 17，但仓库里**刻意不写死任何本机路径**
（写死会让别人 clone 下来直接构建失败）。任选一种方式：

```bash
# 方式一：设置 JAVA_HOME 指向 JDK 17
export JAVA_HOME=/path/to/jdk-17     # Windows: set JAVA_HOME=C:\path\to\jdk-17
./gradlew build

# 方式二：写进你自己的 ~/.gradle/gradle.properties（不会被提交）
#   org.gradle.java.home=/path/to/jdk-17
```

> 如果代理那招不生效（构建报依赖下载超时），改用显式代理：取消 `gradle.properties`
> 里 `systemProp.http/https.proxyHost/Port` 那四行注释，端口填你自己梯子的端口。

### 第三方依赖的坐标

```
mezz.jei:jei-1.20.1-forge:15.49.0.188
dev.ftb.mods:ftb-quests-forge:2001.4.22
dev.ftb.mods:ftb-library-forge:2001.2.13
dev.ftb.mods:ftb-teams-forge:2001.3.2
dev.architectury:architectury-forge:9.2.14
```

FTB 那条线要注意：maven 路径必须带 `/releases/`（`https://maven.ftb.dev/releases`），
裸域名 `https://maven.ftb.dev` 是 404 的。
这些 jar 在 maven 上是按 SRG 名发布的，所以依赖声明外面要套 `fg.deobf(...)`。

**不引入 item-filters**：FTB Quests 2001.4.22 把 `itemfilters` 声明为 `mandatory=false`，
本模组也没有引用它的任何类。

### 为什么 runs 里要加 `-Dmixin.env.disableRefMap=true`

FTB Library 2001.2.13 自带的 refmap 写的是「注解里的官方名 → 生产环境的 SRG 名」，
而 dev 环境里 MC 已经被 ForgeGradle 重映射成官方名；继续套 refmap 就会变成
「在官方名类里找 SRG 名」，`runClient` / `runServer` 直接以
`InvalidInjectionException` 崩掉。关掉 refmap 后 mixin 直接用注解里的官方名，正好命中。
这只影响开发环境运行，玩家侧的生产环境（SRG 名）不受影响。详见 `build.gradle` 末尾的注释。

### 想在 dev 里测「客户端连服务端」还要注意

`gradlew runServer` 生成的 `run/server.properties` 默认是 `online-mode=true`，
而 dev 客户端是离线账号（名字 `Dev`），会被服务端以 `lost connection: Disconnected` 踢掉。
本地调试时把它改成 `online-mode=false` 即可。

另外 FTB 任务书**默认没有绑定按键**，dev 里想打开任务书可以用调试命令，或者先
在「选项 → 按键控制 → FTB 任务」里绑一个；也可以在客户端代码里直接调
`ClientQuestFile.openGui()`。

## 安装

**客户端**：把 `itemchat-1.3.0-forge.jar` 放进 `.minecraft/mods/`。

**服务端**：同一个 jar 也放进服务端的 `mods/` 目录。**这一步是必须的** —— 广播消息需要服务端构造富文本组件，
只有客户端装了的话，别人是看不到按钮的（游戏会提示"当前服务器没有安装 ItemChat"）。

单人游戏不需要额外操作：内置服务端会加载同一个 jar。

## 使用

**分享物品**：打开背包（`E`）或任何容器 → 鼠标移到想展示的物品上 → 按**鼠标中键**。

**分享 JEI 里的物品**：打开 JEI 物品列表（或按 R/U 打开配方界面）→ 鼠标悬停在物品上 → 按**中键**。

**分享 FTB 任务**：打开 FTB 任务书 → 鼠标悬停在某个任务上 → 按**中键**。
（FTB 任务书默认没有绑定按键，需要在「选项 → 按键控制 → FTB 任务」里自己绑一个，或用任务书物品右键。）

想改默认按键？目前中键是写死在代码里的（不占用原版按键绑定），需要改的话修改
`ClientEvents#onMouseButtonPre` 中的
`GLFW.GLFW_MOUSE_BUTTON_MIDDLE` 即可。

## 代码结构

```
src/main/java/com/example/itemchat/
├── ItemChat.java                     模组主类，只负责注册网络
├── net/
│   ├── ItemChatNetwork.java          SimpleChannel，注册四个包
│   ├── ShareItemPacket.java          客户端 -> 服务端：分享物品
│   ├── OpenJeiPacket.java            服务端 -> 客户端：打开 JEI 配方
│   ├── ShareQuestPacket.java         客户端 -> 服务端：分享 FTB 任务
│   └── OpenQuestPacket.java          服务端 -> 客户端：打开 FTB 任务书
├── server/
│   ├── ItemBroadcaster.java          构造物品富文本并广播
│   ├── QuestBroadcaster.java         FTB 任务的软依赖守卫层（没装时降级提示）
│   ├── FtbQuestServerBridge.java     真正接触 FTB 类的那部分（仅在已加载时被触碰）
│   └── ItemChatCommands.java         /itemchat jei|quest 命令
└── client/
    ├── ClientEvents.java             捕获中键（统一使用 InputEvent.MouseButton.Pre）
    ├── ClientPayloadHandler.java     处理两个"打开"包
    ├── JeiIntegration.java           JEI 桥接（打开配方 / 读取鼠标悬停物品）
    ├── ItemChatJeiPlugin.java        JEI 插件入口
    └── FtbQuestsIntegration.java     FTB 桥接（读取鼠标下任务 / 深链打开）
```

## 工作原理

整条链路是「客户端动作 → 服务端校验 → 服务端广播 → 点击回跳」：

```
[客户端] 中键点击物品/任务
   └─ ShareItemPacket / ShareQuestPacket ──► [服务端]
        └─ ItemBroadcaster / FtbQuestServerBridge
           └─ 生成带 HoverEvent / ClickEvent 的 Component，广播给所有玩家
              └─ [客户端] 点击 [检查] / [检查任务]
                   └─ 原版发送命令 /itemchat jei <物品id> 或 /itemchat quest <任务id>
                        └─ [服务端] 校验执行者与目标存在性
                             └─ OpenJeiPacket / OpenQuestPacket ──► [客户端]
                                  └─ 打开本机 JEI 配方 / 本机 FTB 任务书
```

几个踩过的坑，这里都处理了：

1. **为什么必须走自定义网络包？**
   玩家自己发出去的聊天只有纯文本，服务端不会转发 `ClickEvent`。只有服务端构造并下发的组件，
   才能让所有人都看到按钮。

2. **为什么按钮要绕一圈服务端命令？**
   直接在客户端调命令会触发服务器「未知命令」的报错；绕服务端还能顺便校验目标是否存在。

3. **为什么中键统一走 `InputEvent.MouseButton.Pre`，而不是 `ScreenEvent`？**
   最早用的是 `ScreenEvent.MouseButtonPressed.Pre`。它有竞争问题：这个事件在 `Screen#mouseClicked`
   那条链上，很多模组都会监听，一旦有人先 `setCanceled(true)`，Forge 就不会再调用后面没声明
   `receiveCanceled` 的监听器 —— 你的中键就没了。
   实测撞到过：同一份 jar，在 JEI 15.20 的整合包里中键物品正常，在 JEI 15.49 的整合包里就没反应
   （背包槽位和 FTB 任务都还正常）。
   所以改成更底层的 `InputEvent.MouseButton.Pre`：它在 `MouseHandler#onPress` 里、**把点击分发给
   屏幕之前**触发，取消后屏幕（包括 JEI 的浮层）根本收不到这次点击，从根上绕开了"谁先取消"的竞争。
   顺带把三条通道（JEI 物品 / FTB 任务 / 容器槽位）统一到这一个入口，优先级取 `HIGHEST`。

4. **为什么点 [检查] 前要关掉聊天界面？**
   否则退出 JEI 后聊天输入状态会残留，要再按一次 Esc/Enter 才能回到游戏。

5. **为什么 FTB 相关代码要单独拆类？**
   FTB 是软依赖。把真正引用 FTB 类型的代码集中在 `FtbQuestServerBridge` / `FtbQuestsIntegration`
   里，调用点全部先过 `ModList.get().isLoaded("ftbquests")` 守卫，没装 FTB 时这两个类根本不会被加载。

## 许可证

本项目以 **GNU General Public License v3.0（GPL-3.0）** 发布，完整条款见本工程目录中的 [LICENSE](LICENSE)。

你可以使用、修改和再分发本项目，包括商业使用。分发原版或修改版时，须遵守 GPL v3：保留适用的版权和许可证声明，并随附许可证；分发修改版时还须说明修改情况和日期，并按 GPL v3 授权整个受其约束的修改作品。

如果分发编译后的 jar，须按照 GPL v3 第 6 条规定的方式，向接收者提供与该版本对应的完整源码（包括构建所需的脚本）。仅自己修改和使用、不向他人分发时，不要求公开源码；提供源码也不等于必须上传 GitHub。本模组按许可证规定不提供担保。

以上是便于理解的摘要，以 LICENSE 的完整条款为准。Minecraft 和第三方依赖的权利与许可由各自权利人规定，本项目的 GPL 声明不替代其许可，也不自动解决依赖之间的许可兼容性问题。
