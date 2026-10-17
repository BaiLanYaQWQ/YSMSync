# YSMSync

一个面向 Paper/Purpur 1.19.4 的 Yes Steve Model（YSM）服务端插件。

## 功能

- 与安装 YSM 客户端模组的玩家进行协议握手
- 同步玩家模型、模型切换、动画和部分 Molang 数据
- 管理和缓存 `.ysm` 模型文件
- 支持客户端模型上传
- 支持模型同步带宽限制
- 提供 Web 模型管理门户，可上传模型和图片

## 环境要求

- Paper 或 Purpur 1.19.4
- Java 17 或更高版本
- ProtocolLib
- 可选依赖：Images
- 客户端需要安装兼容的 Yes Steve Model 模组

## 安装

1. 将 `YSMSync.jar` 放入服务端的 `plugins` 目录。
2. 安装并启用 ProtocolLib。
3. 启动服务器，插件会生成 `plugins/YSMSync/config.yml`。
4. 根据需要修改模型同步和 Web 门户配置。

不要同时加载其他同名的 YSM 同步插件，以免插件数据目录或网络协议处理发生冲突。

## 命令

管理员可以使用以下命令：

```text
/ysmsync reload
/ysmsync setmodel <玩家> <模型ID> [材质]
```

模型重载权限：

```text
ysmsync.commands.reloadmodels
```

## 从源码构建

构建需要 JDK 21、Gradle，以及 Paper 1.19.4 和 ProtocolLib 的服务端依赖。服务端依赖不会提交到仓库，可以通过 `serverDir` 指定本地服务端目录：

```bash
gradle -PserverDir=/disk2/mcserver/1 shadowJar
```

构建产物位于 `build/libs/YSMSync-1.0.1-1.19.4-port.jar`。

## Web 模型门户

默认配置中 Web 门户监听 `0.0.0.0:25584`，支持模型和图片上传。对外开放前应配置 HTTPS 反向代理，并限制管理账号和访问范围。

## 目录说明

```text
plugins/YSMSync/config.yml       插件配置
plugins/YSMSync/models/          模型文件
plugins/YSMSync/caches/          模型缓存
plugins/YSMSync/player_data/     玩家模型数据
```

## 声明 

该插件是基于 https://github.com/EilsapMC/Noir 开发的非官方衍生项目
核心 YSM 桥接代码来源于 Noir 经过我和gpt的修改
本项目与 CloudXmc、Noir 原作者及 YSM 官方没有合作关系
本项目及其衍生代码按照 GNU GPL v3.0 发布。