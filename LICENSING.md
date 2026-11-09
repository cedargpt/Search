# 许可与历史版本

Copyright (c) 2026 Yisou contributors.

## 修改版说明

本仓库 `cedargpt/Search` 是 [oldjie/yisou](https://github.com/oldjie/yisou) 的修改版，基于原作 0.8.2 之后的工作区改造，版本号 1.2.0（versionCode 21）。相对原作的修改：

- 应用名改为 Search，包名 `org.nofeed.android`，底层沿用原作架构。
- 图标渲染统一为等比铺满 + 圆角矩形遮罩（`AppIconView`）。
- 移除内置 Noto Serif SC 字体，改用系统无衬线字体，并删除对应资源文件。
- 首页网格行高随输入法占用空间自适应，键盘不再遮挡最后一行图标。

原作版权与许可声明全部保留，修改部分同样以 AGPL-3.0-only 授权。分发本修改版时须提供本仓库的完整对应源码。

## 当前版本

自 **0.4.0** 起，本项目原创代码和原创美术采用 **GNU Affero General Public License version 3 only（SPDX：AGPL-3.0-only）**，完整条款见 [LICENSE](LICENSE)。本说明是摘要，不修改标准许可条款。

- 允许遵守许可的使用、修改、再分发和商用。
- 对外分发受许可约束的程序或修改版时，须按条款向接收者提供完整对应源码、构建所需材料和许可，保留版权与变更说明；不能把这些版本仅以闭源包对外出售或分发。
- 对修改版提供网络交互时，还须按第 13 条向相关用户提供对应源码。
- 许可不要求任何时候都把所有内部改动公开到 GitHub；义务依实际分发、修改及网络交互情形触发。不要把本项目称为“禁止一切商用”。
- 程序不附任何担保，具体以许可全文为准。

提供修改版时，请把应用内“本版本源码”入口更新为该修改版的完整对应源码，不能仅链接本项目的原始源码。公开 APK、源码压缩包和 SHA-256 文件在同一 Releases 页面提供；APK 内附完整许可，并可从主应用设置查看。

## 旧版 MIT 权限不会被收回

**0.3.7 及更早版本曾以 MIT 发布**，例如 [v0.3.7 的 LICENSE](https://github.com/oldjie/yisou/blob/v0.3.7/LICENSE)。此次替换当前分支的许可，不追溯撤销先前已授出的 MIT 权限。已取得旧代码的人仍可按旧许可使用旧代码，包括其允许的闭源商用；本次变更不能阻止这一点。后续新增的 AGPL 代码不会因此自动变成 MIT。

旧标签与旧发行附件保留历史许可，不改写为 AGPL。

## 第三方材料

Noto Serif SC 仍采用 SIL OFL 1.1；Gradle Wrapper 仍采用 Apache-2.0；平台名称、图标及其在截图中的外观不因本项目变成 AGPL。详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

参考：[GNU AGPLv3](https://www.gnu.org/licenses/agpl-3.0.html)、[GNU 许可 FAQ](https://www.gnu.org/licenses/gpl-faq.html)。标准全文取自 [SPDX license-list-data](https://github.com/spdx/license-list-data/blob/main/text/AGPL-3.0-only.txt)，未加入“禁止商用”等额外限制。
