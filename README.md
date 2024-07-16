# Open93AtHome

<img src="https://raw.githubusercontent.com/Mxmilu666/bangbang93HUB/main/8Mi_Yile/%E6%88%91%E5%92%8C%E4%BA%B2%E5%A6%B9%E6%9C%80%E8%BF%91.jpg"><br>
**↑万恶之源**

## 简介

这是一个类似于 [OpenBMCLAPI](https://github.com/bangbang93/openbmclapi) **主控端**的分发文件项目
> [!IMPORTANT]
> 本项目实际可被用于分发任何有效 Git 仓库内的文件，因此并不与 bangbang93HUB 有任何关联

## 部署

以下是基于修改现有 OpenBMCLAPI 节点端的办法

### [Node 端](https://github.com/bangbang93/openbmclapi)

| 位置                 | 目的     | 做法                                                                                |
|--------------------|--------|-----------------------------------------------------------------------------------|
| **cluster.ts** L59 | 修改上线地址 | 修改 `private readonly prefixUrl = process.env.CLUSTER_BMCLAPI ??` 后面的值或直接修改传入的环境变量 |

### [Go 端](https://github.com/LiterMC/go-openbmclapi)

| 位置                   | 目的     | 做法                          |
|----------------------|--------|-----------------------------|
| **main.go** L57      | 修改上线地址 | 修改 `const ClusterServerURL` |
| **hijacker.go** L107 | 修改下载地址 | 修改 `const hijackingHost`    |

### [Python 端](https://github.com/TTB-Network/python-openbmclapi)

| 位置                 | 目的     | 做法        |
|--------------------|--------|-----------|
| **config.yml** L15 | 修改上线地址 | 修改 `url:` |

### [CSharp 端](https://github.com/SaltWood-Studio/CSharp-OpenBMCLAPI)

| 位置                          | 目的     | 做法                                                                 |
|-----------------------------|--------|--------------------------------------------------------------------|
| **Modules/HttpRequest.cs**  | 修改上线地址 | 注释 `14 ~ 16`行，在 `12` 行后另起一行，输入</br>`BaseAddress = new Uri("xxxx")` |
| **Modules/Cluster.cs** L704 | 修改下载地址 | 修改 `GetRedirectUrls` 方法调用中插值字符串的值（不加 `/`）                          |

### [PHP 端](https://github.com/AppleBlockTeam/php-openbmclapi)

| 位置                 | 目的     | 做法              |
|--------------------|--------|-----------------|
| **config.php** L18 | 修改上线地址 | 修改 `CenterUrl:` |

## 调试

1. 将此项目 `git clone` 到本地
2. 使用 **IntelliJ IDEA** 打开项目
3. 愉快的开发吧🎉

``` shell
git clone https://github.com/SaltWood-Studio/Open93AtHome.git
cd Open93AtHome
```

### 贡献

提交 PR 前请确保你的代码至少经过编译测试
