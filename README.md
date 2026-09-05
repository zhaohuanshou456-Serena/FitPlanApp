# 健身管家 FitPlanApp

个人健身管理 App（Android · Kotlin · Jetpack Compose · Room）。

## 功能
- **今日/计划**：为某一天编排动作（组数/次数/重量/组间休息），去健身房照单执行、打勾完成；支持复制上一次训练、翻日、回到今天。
- **动作库**：内置常见动作，可按肌群增删改。
- **身体**：录入人体分析仪读数（体重/体脂/肌肉/骨量/水分/BMI/基础代谢/内脏脂肪/腰围），趋势折线图与全览。
- **备份**：JSON 全量导出/恢复，身体参数 CSV 导出。数据仅存本机，不上传。

## 构建（GitHub Actions）
push 到 `main` 后由 `.github/workflows/build.yml` 自动编译，产出的 `app-debug.apk` 可在 Actions 页下载，侧载到 Android / HarmonyOS(4.x) 手机安装。
